package com.yourorg.platform.foculist.tenancy;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.util.StringUtils;

import org.springframework.web.client.RestTemplate;

class JwtClaimExtractor {
    private final SecretKey signingKey;
    private final String identityUrl;
    private final RestTemplate restTemplate;

    JwtClaimExtractor(String jwtSecret, String identityUrl, RestTemplate restTemplate) {
        this.signingKey = parseSigningKey(jwtSecret);
        this.identityUrl = identityUrl;
        this.restTemplate = restTemplate;
    }

    String extractClaim(String authorizationHeader, String claimName) {
        if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }

        String token = authorizationHeader.substring("Bearer ".length()).trim();
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            String jti = claims.getId();
            if (StringUtils.hasText(jti) && isBlacklisted(jti)) {
                 throw new JwtValidationException("Token has been revoked");
            }

            Object raw = claims.get(claimName);
            return raw == null ? null : raw.toString();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new JwtValidationException("Invalid or expired JWT token");
        }
    }

    private boolean isBlacklisted(String jti) {
        if (identityUrl == null || restTemplate == null) return false;
        try {
            return Boolean.TRUE.equals(restTemplate.getForObject(identityUrl + "/api/identity/v1/auth/blacklist-check?jti=" + jti, Boolean.class));
        } catch (Exception ex) {
            return false;
        }
    }

    private SecretKey parseSigningKey(String jwtSecret) {
        if (!StringUtils.hasText(jwtSecret)) {
            throw new IllegalStateException("app.security.jwt.secret must be configured");
        }
        byte[] keyBytes = decode(jwtSecret.trim());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private byte[] decode(String secret) {
        try {
            return Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException ignored) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }
}
