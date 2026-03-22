package com.yourorg.platform.foculist.identity.clean.infrastructure.grpc;

import com.yourorg.platform.foculist.identity.grpc.*;
import com.yourorg.platform.foculist.identity.clean.application.service.AuthService;
import com.yourorg.platform.foculist.identity.clean.application.service.UserService;
import com.yourorg.platform.foculist.identity.clean.infrastructure.security.JwtService;
import io.grpc.stub.StreamObserver;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import java.util.Collections;
import java.util.List;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class GrpcIdentityService extends IdentityServiceGrpc.IdentityServiceImplBase {
    private final AuthService authService;
    private final UserService userService;
    private final JwtService jwtService;

    @Override
    public void authenticate(AuthRequest request, StreamObserver<AuthResponse> responseObserver) {
        try {
            AuthService.LoginResult result = authService.login(request.getTenantId(), request.getEmail(), request.getPassword(), false);
            
            if (result instanceof AuthService.AuthTokens tokens) {
                AuthResponse response = AuthResponse.newBuilder()
                        .setAccessToken(tokens.accessToken())
                        .setRefreshToken(tokens.refreshToken())
                        .setExpiresIn(3600) // Mocked expiry for now
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
            } else if (result instanceof AuthService.MfaChallenge) {
                responseObserver.onError(io.grpc.Status.UNIMPLEMENTED
                        .withDescription("MFA is required but not yet supported over gRPC")
                        .asRuntimeException());
            }
        } catch (Exception e) {
            log.error("gRPC Authentication failed for user: {}", request.getEmail(), e);
            responseObserver.onError(io.grpc.Status.UNAUTHENTICATED
                    .withDescription("Invalid credentials: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void validateToken(ValidationRequest request, StreamObserver<ValidationResponse> responseObserver) {
        try {
            Claims claims = jwtService.parseToken(request.getToken());
            String userId = claims.get("userId", String.class);
            String tenantId = claims.get("tenant", String.class);
            String email = claims.getSubject();
            
            // Extract roles - currently simplified
            String role = claims.get("role", String.class);
            List<String> roles = role != null ? List.of(role) : Collections.emptyList();

            ValidationResponse response = ValidationResponse.newBuilder()
                    .setValid(true)
                    .setUserId(userId)
                    .setEmail(email)
                    .setTenantId(tenantId)
                    .addAllRoles(roles)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onNext(ValidationResponse.newBuilder().setValid(false).build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getUserPermissions(PermissionRequest request, StreamObserver<PermissionResponse> responseObserver) {
        // Placeholder for RBAC permission resolution
        // In a real Google-grade L5 system, this would fetch from a cache or the RBAC DB
        PermissionResponse response = PermissionResponse.newBuilder()
                .addAllPermissions(List.of("TASK_READ", "TASK_WRITE", "PROJECT_READ"))
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
