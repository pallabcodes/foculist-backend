package com.yourorg.platform.foculist.tenancy.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.Serializable;

@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {

    private static final Logger log = LoggerFactory.getLogger(CustomPermissionEvaluator.class);

    private final RestTemplate restTemplate;
    private final String identityServiceUrl;

    public CustomPermissionEvaluator(
            @Value("${app.identity-service.url:http://localhost:8081}") String identityServiceUrl) {
        this.restTemplate = new RestTemplate();
        this.identityServiceUrl = identityServiceUrl;
    }

    @Override
    public boolean hasPermission(Authentication auth, Object targetDomainObject, Object permission) {
        if ((auth == null) || (targetDomainObject == null) || !(permission instanceof String)) {
            return false;
        }
        String targetType = targetDomainObject.getClass().getSimpleName();
        String targetId = targetDomainObject.toString();
        return hasPrivilege(auth, targetType, targetId, permission.toString());
    }

    @Override
    public boolean hasPermission(Authentication auth, Serializable targetId, String targetType, Object permission) {
        if ((auth == null) || (targetType == null) || (targetId == null) || !(permission instanceof String)) {
            return false;
        }
        return hasPrivilege(auth, targetType, targetId.toString(), permission.toString());
    }

    private boolean hasPrivilege(Authentication auth, String resourceType, String resourceId, String permission) {
        if (!(auth.getPrincipal() instanceof Jwt jwt)) {
            log.warn("Authentication principal is not a JWT: {}", auth.getPrincipal());
            return false;
        }

        String userId = jwt.getSubject();
        String orgRole = jwt.getClaimAsString("orgRole");

        // Org admins implicitly have all permissions within the tenant
        if ("ADMIN".equalsIgnoreCase(orgRole)) {
            return true;
        }

        try {
            if (identityServiceUrl == null) {
                log.error("Identity service URL is not configured");
                return false;
            }
            String url = UriComponentsBuilder.fromHttpUrl(identityServiceUrl)
                    .path("/v1/grants/check")
                    .queryParam("userId", userId)
                    .queryParam("resourceType", resourceType)
                    .queryParam("resourceId", resourceId)
                    .queryParam("requiredLevel", permission)
                    .toUriString();

            ResponseEntity<Boolean> response = restTemplate.getForEntity(url, Boolean.class);
            return Boolean.TRUE.equals(response.getBody());

        } catch (Exception e) {
            log.error("Failed to check permission against identity-service", e);
            return false;
        }
    }
}
