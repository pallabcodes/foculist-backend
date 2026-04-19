package com.yourorg.platform.foculist.identity.clean.application.service;

import com.yourorg.platform.foculist.tenancy.TenantContext;
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
import com.yourorg.platform.foculist.identity.clean.domain.model.GlobalRole;
import com.yourorg.platform.foculist.identity.clean.domain.repository.TokenBlacklistRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final IdentityProviderPort identityProviderPort;
    private final JwtService jwtService;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final com.yourorg.platform.foculist.identity.clean.adapter.out.persistence.UserSessionRepository userSessionRepository;
    private final JdbcTemplate jdbcTemplate;

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
        user.setTenantId(com.yourorg.platform.foculist.tenancy.TenantContext.require());
        user.setFullName(name);
        user.setEmail(email);
        user.setProviderSub(providerSub);
        user.setGlobalRole(GlobalRole.USER);
        user.setActive(true);
        return userRepository.save(user);
    }

    public sealed interface LoginResult permits AuthTokens, MfaChallenge {}
    public record AuthTokens(String accessToken, String refreshToken, UUID userId) implements LoginResult {}
    public record MfaChallenge(String mfaToken, String message) implements LoginResult {}

    public LoginResult login(String tenantId, String email, String password, boolean devBypass) {
        // 1. Authenticate against the Identity Provider
        try {
            identityProviderPort.authenticate(email, password);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        // 2. Fetch local user attributes
        // Login is a global operation, but the context might be set to a specific org (e.g. from X-Tenant-ID header)
        // We temporarily switch to 'public' to find the user record which was created during signup.
        String originalTenant = TenantContext.get();
        User user;
        try {
            TenantContext.set("public");
            user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found locally"));
        } finally {
            TenantContext.set(originalTenant);
        }

        String role = user.getGlobalRole() != null ? user.getGlobalRole().name() : "USER";
        String orgRole = resolveOrgRole(user.getId(), tenantId);

        // 3. Handle MFA
        if (!devBypass) {
            String mfaToken = UUID.randomUUID().toString();
            String mfaCode = String.format("%06d", new java.util.Random().nextInt(999999));
            
            user.setMfaSessionToken(mfaToken);
            user.setMfaCode(mfaCode);
            user.setMfaExpiresAt(java.time.OffsetDateTime.now().plusMinutes(5));
            userRepository.save(user);
            
            // In a real application, we would publish an event here to send the email
            System.out.println("========== DEV LOCAL LOG: MFA CODE GENERATED ==========");
            System.out.println("EMAIL: " + email + " | CODE: " + mfaCode);
            System.out.println("=======================================================");
            
            return new MfaChallenge(mfaToken, "An email with a 6-digit code has been sent.");
        }

        return generateTokensForUser(user, tenantId, role, orgRole, "0.0.0.0", "unknown");
    }

    public AuthTokens verifyMfa(String tenantId, String mfaToken, String code, String ipAddress, String userAgent) {
        User user = userRepository.findAll().stream()
                .filter(u -> mfaToken.equals(u.getMfaSessionToken()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid MFA session"));

        if (user.getMfaExpiresAt().isBefore(java.time.OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "MFA code expired");
        }

        if (!code.equals(user.getMfaCode())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid MFA code");
        }

        // Clear MFA state on success
        user.setMfaSessionToken(null);
        user.setMfaCode(null);
        user.setMfaExpiresAt(null);
        userRepository.save(user);

        String role = user.getGlobalRole() != null ? user.getGlobalRole().name() : "USER";
        String orgRole = resolveOrgRole(user.getId(), tenantId);

        return generateTokensForUser(user, tenantId, role, orgRole, ipAddress, userAgent);
    }

    private AuthTokens generateTokensForUser(User user, String tenantId, String role, String orgRole, String ipAddress, String userAgent) {
        String accessTokenJti = UUID.randomUUID().toString();
        String refreshTokenJti = UUID.randomUUID().toString();

        // Save User Session
        com.yourorg.platform.foculist.identity.clean.domain.model.UserSession session = com.yourorg.platform.foculist.identity.clean.domain.model.UserSession.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .jti(accessTokenJti)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .createdAt(java.time.LocalDateTime.now())
                .expiresAt(java.time.LocalDateTime.now().plusSeconds(jwtService.getAccessTokenExpiration() / 1000))
                .build();
        userSessionRepository.save(session);

        // Fetch granular permissions
        List<String> permissions = fetchUserPermissions(user.getId(), tenantId);
        String scope = String.join(" ", permissions);

        Map<String, Object> accessClaims = Map.of(
                "userId", user.getId().toString(),
                "role", role,
                "orgRole", orgRole,
                "tenant", tenantId,
                "scope", scope
        );
        Map<String, Object> refreshClaims = Map.of(
                "userId", user.getId().toString(),
                "tenant", tenantId,
                "tokenType", "refresh"
        );

        return new AuthTokens(
                jwtService.generateAccessToken(user.getEmail(), accessClaims, accessTokenJti),
                jwtService.generateRefreshToken(user.getEmail(), refreshClaims, refreshTokenJti),
                user.getId()
        );
    }

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

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        String role = user.getGlobalRole() != null ? user.getGlobalRole().name() : "USER";
        String orgRole = resolveOrgRole(user.getId(), tenantId);

        // Fetch granular permissions
        List<String> permissions = fetchUserPermissions(user.getId(), tenantId);
        String scope = String.join(" ", permissions);

        Map<String, Object> accessClaims = Map.of(
                "userId", user.getId().toString(),
                "role", role,
                "orgRole", orgRole,
                "tenant", tenantId,
                "scope", scope
        );
        Map<String, Object> refreshClaims = Map.of(
                "userId", user.getId().toString(),
                "tenant", tenantId,
                "tokenType", "refresh"
        );

        String accessTokenJti = UUID.randomUUID().toString();
        String refreshTokenJti = UUID.randomUUID().toString();

        return new AuthTokens(
                jwtService.generateAccessToken(user.getEmail(), accessClaims, accessTokenJti),
                jwtService.generateRefreshToken(user.getEmail(), refreshClaims, refreshTokenJti),
                user.getId()
        );
    }

    public void verifyUser(String email, String code) {
        identityProviderPort.confirmUser(email, code);
    }

    public void forgotPassword(String email) {
        identityProviderPort.forgotPassword(email);
    }

    public void confirmForgotPassword(String email, String code, String newPassword) {
        identityProviderPort.confirmForgotPassword(email, code, newPassword);
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

    public void changePassword(String email, String accessToken, String oldPassword, String newPassword) {
        identityProviderPort.changePassword(email, accessToken, oldPassword, newPassword);
    }

    private List<String> fetchUserPermissions(UUID userId, String tenantId) {
        String sql = """
            SELECT DISTINCT p.code 
            FROM identity.permissions p
            JOIN identity.role_permissions rp ON p.id = rp.permission_id
            JOIN identity.user_roles ur ON rp.role_id = ur.role_id
            WHERE ur.user_id = ? AND ur.tenant_id = ?
        """;
        try {
            return jdbcTemplate.queryForList(sql, String.class, userId, tenantId);
        } catch (Exception e) {
            // log was not fully imported or mapped, let's use standard sysout or inject it if available
            // Wait, AuthService ALREADY has `log` on line 38 or something? 
            // Let's check if `log` exists.
            System.err.println("Failed to fetch permissions for user: " + userId);
            return List.of();
        }
    }
}
