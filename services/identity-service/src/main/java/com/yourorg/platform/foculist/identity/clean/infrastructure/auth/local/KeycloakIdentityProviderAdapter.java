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
            // Because Keycloak might conflict on existing email, we catch and return a fake ID if testing locally
            log.error("Failed to register user to Keycloak", e);
            throw new RuntimeException("Failed to register user in local Keycloak", e);
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
            throw new RuntimeException("Failed to authenticate with Keycloak", e);
        }
    }

    @Override
    public void confirmUser(String email, String confirmationCode) {
        log.info("Local Keycloak: Auto-confirming user for offline dev");
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
        log.info("Local Keycloak: Forgot password triggered for {}", email);
    }

    @Override
    public void confirmForgotPassword(String email, String confirmationCode, String newPassword) {
        log.info("Local Keycloak: Resetting password for offline dev");
    }
}
