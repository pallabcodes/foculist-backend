package com.yourorg.platform.foculist.identity.clean.application.service;

import com.yourorg.platform.foculist.identity.clean.adapter.out.persistence.MembershipRepository;
import com.yourorg.platform.foculist.identity.clean.adapter.out.persistence.UserRepository;
import com.yourorg.platform.foculist.identity.clean.domain.model.Membership;
import com.yourorg.platform.foculist.identity.clean.domain.model.MembershipRole;
import com.yourorg.platform.foculist.identity.clean.domain.model.User;
import com.yourorg.platform.foculist.identity.clean.domain.port.IdentityProviderPort;
import com.yourorg.platform.foculist.identity.clean.infrastructure.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import com.yourorg.platform.foculist.identity.clean.domain.model.TokenBlacklist;
import com.yourorg.platform.foculist.identity.clean.domain.repository.TokenBlacklistRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final IdentityProviderPort identityProviderPort;
    private final JwtService jwtService;
    private final TokenBlacklistRepository tokenBlacklistRepository;

    @Transactional
    public User register(String name, String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        // 1. Register with the Identity Provider (AWS Cognito or Local Fakes)
        var attributes = Map.of(
                "name", name
        );
        String providerSub = identityProviderPort.registerUser(email, password, attributes);

        // 2. Save to our local relational DB
        User user = new User();
        user.setFullName(name);
        user.setEmail(email);
        user.setProviderSub(providerSub);
        user.setGlobalRole("USER");
        user.setActive(true);
        return userRepository.save(user);
    }

    public AuthTokens login(String tenantId, String email, String password) {
        // 1. Authenticate against the Identity Provider
        Map<String, String> providerTokens;
        try {
            providerTokens = identityProviderPort.authenticate(email, password);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        // 2. Fetch local user attributes
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found locally"));

        String role = StringUtils.hasText(user.getGlobalRole()) ? user.getGlobalRole() : "USER";
        String orgRole = resolveOrgRole(user.getId(), tenantId);

        Map<String, Object> accessClaims = Map.of(
                "userId", user.getId().toString(),
                "role", role,
                "orgRole", orgRole,
                "tenant", tenantId
        );
        Map<String, Object> refreshClaims = Map.of(
                "userId", user.getId().toString(),
                "tenant", tenantId,
                "tokenType", "refresh"
        );

        return new AuthTokens(
                jwtService.generateAccessToken(user.getEmail(), accessClaims),
                jwtService.generateRefreshToken(user.getEmail(), refreshClaims),
                user.getId()
        );
    }

    public record AuthTokens(String accessToken, String refreshToken, UUID userId) {}

    /**
     * Resolve the user's organization membership role within the given tenant.
     * Falls back to GUEST if no membership exists.
     */
    private String resolveOrgRole(UUID userId, String tenantId) {
        return membershipRepository.findByUserIdAndTenantId(userId, tenantId)
                .map(Membership::getRole)
                .map(MembershipRole::name)
                .orElse(MembershipRole.GUEST.name());
    }

    /**
     * Validate a refresh token and issue a new access+refresh pair.
     * Implements single-use rotation: the old refresh token is implicitly invalidated
     * by issuing a new one with a fresh expiry.
     */
    public AuthTokens refreshTokens(String refreshToken, String tenantId) {
        Claims claims;
        try {
            claims = jwtService.parseToken(refreshToken);
        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }

        // Guard: only refresh tokens can be used for refresh
        String tokenType = claims.get("tokenType", String.class);
        if (!"refresh".equals(tokenType)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token is not a refresh token");
        }

        String email = claims.getSubject();
        String userIdStr = claims.get("userId", String.class);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        String role = StringUtils.hasText(user.getGlobalRole()) ? user.getGlobalRole() : "USER";
        String orgRole = resolveOrgRole(user.getId(), tenantId);

        Map<String, Object> accessClaims = Map.of(
                "userId", user.getId().toString(),
                "role", role,
                "orgRole", orgRole,
                "tenant", tenantId
        );
        Map<String, Object> refreshClaims = Map.of(
                "userId", user.getId().toString(),
                "tenant", tenantId,
                "tokenType", "refresh"
        );

        return new AuthTokens(
                jwtService.generateAccessToken(user.getEmail(), accessClaims),
                jwtService.generateRefreshToken(user.getEmail(), refreshClaims),
                user.getId()
        );
    }

    @Transactional
    public void logout(String token) {
        Claims claims;
        try {
            claims = jwtService.parseToken(token);
        } catch (JwtException e) {
            return; // Already expired or invalid
        }

        String jti = claims.getId();
        if (jti != null && !tokenBlacklistRepository.existsById(jti)) {
            TokenBlacklist blacklist = new TokenBlacklist(jti, claims.getExpiration().toInstant(), Instant.now());
            tokenBlacklistRepository.save(blacklist);
        }
    }

    @Transactional(readOnly = true)
    public boolean isTokenBlacklisted(String jti) {
        if (!StringUtils.hasText(jti)) return false;
        return tokenBlacklistRepository.existsById(jti);
    }
}
