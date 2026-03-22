package com.yourorg.platform.foculist.identity.web;

import com.yourorg.platform.foculist.identity.clean.application.service.AuthService;
import com.yourorg.platform.foculist.identity.clean.application.service.UserService;
import com.yourorg.platform.foculist.identity.clean.adapter.out.persistence.MembershipRepository;
import com.yourorg.platform.foculist.identity.clean.domain.model.Membership;
import com.yourorg.platform.foculist.identity.clean.domain.model.MembershipRole;
import com.yourorg.platform.foculist.identity.clean.domain.model.User;
import com.yourorg.platform.foculist.tenancy.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1")
@Validated
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final UserService userService;
    private final MembershipRepository membershipRepository;
    private final com.yourorg.platform.foculist.identity.clean.application.service.UserSessionService userSessionService;

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "X-Dev-Bypass", defaultValue = "false") boolean devBypass
    ) {
        String tenantId = TenantContext.require();
        AuthService.LoginResult result = authService.login(tenantId, request.email(), request.password(), devBypass);
        
        if (result instanceof AuthService.AuthTokens tokens) {
            return ResponseEntity.ok(new TokensResponse(tokens.accessToken(), tokens.refreshToken(), tokens.userId(), tenantId));
        } else if (result instanceof AuthService.MfaChallenge challenge) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                    new MfaChallengeResponse(true, challenge.mfaToken(), challenge.message())
            );
        }
        throw new IllegalStateException("Unknown login result type");
    }

    @PostMapping("/auth/mfa/verify")
    public ResponseEntity<TokensResponse> verifyMfa(@Valid @RequestBody MfaVerifyRequest request) {
        String tenantId = TenantContext.require();
        AuthService.AuthTokens tokens = authService.verifyMfa(tenantId, request.mfaToken(), request.code(), "0.0.0.0", "unknown");
        return ResponseEntity.ok(new TokensResponse(tokens.accessToken(), tokens.refreshToken(), tokens.userId(), tenantId));
    }

    @PostMapping("/auth/refresh")
    public TokensResponse refresh(@Valid @RequestBody RefreshRequest request) {
        String tenantId = TenantContext.require();
        AuthService.AuthTokens tokens = authService.refreshTokens(request.refreshToken(), tenantId);
        return new TokensResponse(tokens.accessToken(), tokens.refreshToken(), tokens.userId(), tenantId);
    }

    @PostMapping("/auth/verify")
    public ResponseEntity<SuccessMessage> verify(@Valid @RequestBody VerifyRequest request) {
        authService.verifyUser(request.email(), request.code());
        return ResponseEntity.ok(new SuccessMessage("User verified successfully"));
    }

    @PostMapping("/auth/forgot-password")
    public ResponseEntity<SuccessMessage> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.ok(new SuccessMessage("Password reset email sent"));
    }

    @PostMapping("/auth/forgot-password/confirm")
    public ResponseEntity<SuccessMessage> confirmForgotPassword(@Valid @RequestBody ConfirmForgotPasswordRequest request) {
        authService.confirmForgotPassword(request.email(), request.code(), request.newPassword());
        return ResponseEntity.ok(new SuccessMessage("Password has been reset successfully"));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<SuccessMessage> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            authService.logout(token);
        }
        return ResponseEntity.ok(new SuccessMessage("Logged out successfully"));
    }

    @PostMapping("/auth/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request.name(), request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(toUserResponse(user));
    }

    @GetMapping("/auth/blacklist-check")
    public ResponseEntity<Boolean> isBlacklisted(@RequestParam String jti) {
        boolean blacklisted = authService.isTokenBlacklisted(jti);
        return ResponseEntity.ok(blacklisted);
    }

    @GetMapping("/user")
    public UserResponse getCurrentUser(
            @RequestParam(required = false) String email,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String resolvedEmail = resolveEmail(email, jwt);
        User user = userService.findUserByEmail(resolvedEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return toUserResponse(user);
    }

    @PostMapping("/user")
    public ResponseEntity<CreateUserResponse> createUserCompatibility(
            @Valid @RequestBody CreateUserCompatibilityRequest request
    ) {
        String password = request.password() == null || request.password().isBlank()
                ? "temp-" + UUID.randomUUID()
                : request.password();
        User user = authService.register(request.name(), request.email(), password);

        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateUserResponse(
                "User created successfully",
                toUserResponse(user)
        ));
    }

    @GetMapping("/users/{id}")
    public UserResponse getUser(@PathVariable UUID id) {
        User user = userService.getUserById(id);
        return toUserResponse(user);
    }

    @PutMapping("/user")
    public ResponseEntity<SuccessMessage> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String email = resolveEmail(null, jwt);
        User user = userService.findUserByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        userService.updateProfile(user.getId(), request.name());
        return ResponseEntity.ok(new SuccessMessage("Profile updated successfully"));
    }

    @PostMapping("/user/password")
    public ResponseEntity<SuccessMessage> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String email = resolveEmail(null, jwt);
        String accessToken = jwt.getTokenValue();
        
        authService.changePassword(email, accessToken, request.oldPassword(), request.newPassword());
        return ResponseEntity.ok(new SuccessMessage("Password changed successfully"));
    }

    @GetMapping("/user/sessions")
    public ResponseEntity<java.util.List<com.yourorg.platform.foculist.identity.clean.domain.model.UserSession>> getUserSessions(
            @AuthenticationPrincipal Jwt jwt
    ) {
        String email = resolveEmail(null, jwt);
        User user = userService.findUserByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        return ResponseEntity.ok(userSessionService.getUserSessions(user.getId()));
    }

    @DeleteMapping("/user/sessions/{jti}")
    public ResponseEntity<SuccessMessage> revokeSession(
            @PathVariable String jti,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String email = resolveEmail(null, jwt);
        User user = userService.findUserByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        userSessionService.revokeSession(jti, user.getId());
        return ResponseEntity.ok(new SuccessMessage("Session revoked successfully"));
    }

    private String resolveEmail(String email, Jwt jwt) {
        if (StringUtils.hasText(email)) {
            return email;
        }
        if (jwt != null && StringUtils.hasText(jwt.getSubject())) {
            return jwt.getSubject();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user is required");
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record RegisterRequest(@NotBlank String name, @Email String email, @NotBlank String password) {}
    public record CreateUserCompatibilityRequest(@NotBlank String name, @Email String email, String password) {}
    public record RefreshRequest(@NotBlank String refreshToken) {}
    public record VerifyRequest(@NotBlank @Email String email, @NotBlank String code) {}
    public record ForgotPasswordRequest(@NotBlank @Email String email) {}
    public record ConfirmForgotPasswordRequest(@NotBlank @Email String email, @NotBlank String code, @NotBlank String newPassword) {}
    public record MfaVerifyRequest(@NotBlank String mfaToken, @NotBlank String code) {}
    public record MfaChallengeResponse(boolean requiresMfa, String mfaToken, String message) {}
    public record UpdateProfileRequest(@NotBlank String name) {}
    public record ChangePasswordRequest(@NotBlank String oldPassword, @NotBlank String newPassword) {}

    public record TokensResponse(String accessToken, String refreshToken, UUID userId, String tenantId) {}

    public record UserResponse(UUID id, String name, String email, String role, String orgRole, String tenantId) {}

    public record CreateUserResponse(String message, UserResponse user) {}

    public record SuccessMessage(String message) {}

    private UserResponse toUserResponse(User user) {
        String tenantId = TenantContext.get();
        if (tenantId == null) {
            tenantId = "public"; // Fallback
        }
        
        String orgRole = MembershipRole.GUEST.name();
        try {
            var membership = membershipRepository.findByUserIdAndTenantId(user.getId(), tenantId);
            if (membership.isPresent() && membership.get() != null) {
                var role = membership.get().getRole();
                if (role != null) {
                    orgRole = role.name();
                }
            }
        } catch (Exception e) {
            // Ignore membership lookup errors for toUserResponse
        }

        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getGlobalRole() != null ? user.getGlobalRole().name() : "USER",
                orgRole,
                tenantId
        );
    }
}
