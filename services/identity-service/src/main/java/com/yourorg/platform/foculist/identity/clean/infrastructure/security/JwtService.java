package com.yourorg.platform.foculist.identity.clean.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    @Value("${app.security.jwt.secret:Zm9jdWxpc3QtZGV2LWp3dC1zZWNyZXQta2VlcC1jaGFuZ2U=}")
    private String secretKey;

    @Value("${app.security.jwt.access-expiration:900000}")
    private long accessTokenExpiration;

    @Value("${app.security.jwt.refresh-expiration:2592000000}")
    private long refreshTokenExpiration;

    public String generateAccessToken(String username, Map<String, Object> extraClaims) {
        return buildToken(extraClaims, username, accessTokenExpiration);
    }

    public String generateRefreshToken(String username, Map<String, Object> extraClaims) {
        return buildToken(extraClaims, username, refreshTokenExpiration);
    }

    public String generateToken(String username, Map<String, Object> extraClaims) {
        return generateAccessToken(username, extraClaims);
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expiration) {
        return Jwts.builder()
                .id(java.util.UUID.randomUUID().toString())
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private byte[] decode(String secret) {
        try {
            return Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException ignored) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * Parse and validate a JWT token, returning its claims.
     * @throws JwtException if the token is expired, malformed, or has an invalid signature
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Returns true if the token is structurally valid and not expired.
     */
    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
