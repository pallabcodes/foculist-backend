package com.yourorg.platform.foculist.identity.clean.infrastructure.auth.local;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourorg.platform.foculist.identity.clean.domain.port.IdentityProviderPort;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class KeycloakIdentityProviderAdapter implements IdentityProviderPort {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(KeycloakIdentityProviderAdapter.class);

    private final String serverUrl;
    private final String realm;
    private final String clientId;
    private final String clientSecret;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public KeycloakIdentityProviderAdapter(String serverUrl, String realm, String clientId, String clientSecret) {
        this.serverUrl = serverUrl;
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.restTemplate = new RestTemplate();
    }

    private String getAdminToken() {
        String tokenUrl = String.format("%s/realms/master/protocol/openid-connect/token", serverUrl);
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", "admin-cli");
        body.add("username", "admin");
        body.add("password", "admin");
        body.add("grant_type", "password");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        org.springframework.http.HttpEntity<MultiValueMap<String, String>> request = new org.springframework.http.HttpEntity<>(body, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);
        
        try {
            Map<String, Object> map = mapper.readValue(response.getBody(), new TypeReference<>() {});
            return (String) map.get("access_token");
        } catch (Exception e) {
            throw new RuntimeException("Failed to get Keycloak admin token", e);
        }
    }

    @Override
    public String registerUser(String email, String password, Map<String, String> attributes) {
        String adminToken = getAdminToken();
        String usersUrl = String.format("%s/admin/realms/%s/users", serverUrl, realm);

        Map<String, Object> userRepresentation = new HashMap<>();
        userRepresentation.put("username", email);
        userRepresentation.put("email", email);
        userRepresentation.put("enabled", true);
        userRepresentation.put("emailVerified", true);
        
        if (attributes.containsKey("name")) {
            userRepresentation.put("firstName", attributes.get("name"));
        }
        userRepresentation.put("requiredActions", new String[0]);

        Map<String, Object> credential = new HashMap<>();
        credential.put("type", "password");
        credential.put("value", password);
        credential.put("temporary", false);

        userRepresentation.put("credentials", new Object[]{credential});

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        org.springframework.http.HttpEntity<Map<String, Object>> request = new org.springframework.http.HttpEntity<>(userRepresentation, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(usersUrl, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                 // Location header contains the new user ID
                var location = response.getHeaders().getLocation();
                if (location != null) {
                    String path = location.getPath();
                    return path.substring(path.lastIndexOf('/') + 1);
                }
                return UUID.randomUUID().toString(); // Fallback
            } else {
                throw new RuntimeException("Failed to create user in Keycloak: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to register user to Keycloak", e);
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "Failed to register user in identity provider", e);
        }
    }

    @Override
    public Map<String, String> authenticate(String email, String password) {
        String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", serverUrl, realm);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("username", email);
        body.add("password", password);
        body.add("grant_type", "password");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        org.springframework.http.HttpEntity<MultiValueMap<String, String>> request = new org.springframework.http.HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);
            Map<String, Object> resMap = mapper.readValue(response.getBody(), new TypeReference<>() {});

            return Map.of(
                    "accessToken", (String) resMap.get("access_token"),
                    "idToken", resMap.containsKey("id_token") ? (String) resMap.get("id_token") : "none",
                    "refreshToken", (String) resMap.get("refresh_token"),
                    "expiresIn", String.valueOf(resMap.get("expires_in"))
            );
        } catch (Exception e) {
            log.error("Failed to authenticate with Keycloak: {}", e.getMessage());
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid credentials or identity provider unreachable", e);
        }
    }

    @Override
    public void confirmUser(String email, String confirmationCode) {
        log.info("confirmUser not strictly required for keycloak password-based flow yet.");
    }

    @Override
    public Map<String, String> refreshToken(String refreshToken) {
        String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", serverUrl, realm);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", refreshToken);
        body.add("grant_type", "refresh_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        org.springframework.http.HttpEntity<MultiValueMap<String, String>> request = new org.springframework.http.HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);
            Map<String, Object> resMap = mapper.readValue(response.getBody(), new TypeReference<>() {});

            return Map.of(
                    "accessToken", (String) resMap.get("access_token"),
                    "idToken", resMap.containsKey("id_token") ? (String) resMap.get("id_token") : "none",
                    "expiresIn", String.valueOf(resMap.get("expires_in"))
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to refresh token with Keycloak", e);
        }
    }

    @Override
    public void forgotPassword(String email) {
        log.info("ForgotPassword flow triggered for {}", email);
        // Implement real Keycloak forgot password via Admin REST API if required
    }

    @Override
    public void confirmForgotPassword(String email, String confirmationCode, String newPassword) {
        log.info("ConfirmForgotPassword flow triggered");
        // Implement real Keycloak confirm password via Admin REST API if required
    }

    @Override
    public void changePassword(String email, String accessToken, String oldPassword, String newPassword) {
        // 1. Verify old password first
        try {
            authenticate(email, oldPassword);
        } catch (Exception e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Current password verification failed");
        }

        // 2. Get Admin token and lookup user
        try {
            String adminToken = getAdminToken();
            String searchUrl = String.format("%s/admin/realms/%s/users?email=%s", serverUrl, realm, email);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            org.springframework.http.HttpEntity<Void> request = new org.springframework.http.HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    searchUrl, org.springframework.http.HttpMethod.GET, request, String.class);

            java.util.List<Map<String, Object>> users = mapper.readValue(
                    response.getBody(), new TypeReference<>() {});

            if (users.isEmpty()) {
                throw new RuntimeException("User not found in Keycloak for email: " + email);
            }

            String userId = (String) users.get(0).get("id");
            String resetUrl = String.format("%s/admin/realms/%s/users/%s/reset-password", serverUrl, realm, userId);

            // 3. Reset password
            Map<String, Object> credential = new HashMap<>();
            credential.put("type", "password");
            credential.put("value", newPassword);
            credential.put("temporary", false);

            org.springframework.http.HttpEntity<Map<String, Object>> resetRequest = new org.springframework.http.HttpEntity<>(credential, headers);
            restTemplate.exchange(resetUrl, org.springframework.http.HttpMethod.PUT, resetRequest, Void.class);

            log.info("Password successfully updated for user {} inside Keycloak", email);

        } catch (Exception e) {
            log.error("Failed to update password in Keycloak for {}", email, e);
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update password in identity provider", e);
        }
    }
}
