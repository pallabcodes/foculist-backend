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

    @PostMapping("/auth/login")
    public TokensResponse login(@Valid @RequestBody LoginRequest request) {
        String tenantId = TenantContext.require();
        AuthService.AuthTokens tokens = authService.login(tenantId, request.email(), request.password());
        return new TokensResponse(tokens.accessToken(), tokens.refreshToken(), tokens.userId(), tenantId);
    }

    @PostMapping("/auth/refresh")
    public TokensResponse refresh(@Valid @RequestBody RefreshRequest request) {
        String tenantId = TenantContext.require();
        AuthService.AuthTokens tokens = authService.refreshTokens(request.refreshToken(), tenantId);
        return new TokensResponse(tokens.accessToken(), tokens.refreshToken(), tokens.userId(), tenantId);
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            authService.logout(token);
        }
        return ResponseEntity.noContent().build();
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

    public record TokensResponse(String accessToken, String refreshToken, UUID userId, String tenantId) {}

    public record UserResponse(UUID id, String name, String email, String role, String orgRole, String tenantId) {}

    public record CreateUserResponse(String message, UserResponse user) {}

    private UserResponse toUserResponse(User user) {
        String tenantId = TenantContext.require();
        String orgRole = membershipRepository.findByUserIdAndTenantId(user.getId(), tenantId)
                .map(Membership::getRole)
                .map(MembershipRole::name)
                .orElse(MembershipRole.GUEST.name());
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getGlobalRole(),
                orgRole,
                tenantId
        );
    }
}
