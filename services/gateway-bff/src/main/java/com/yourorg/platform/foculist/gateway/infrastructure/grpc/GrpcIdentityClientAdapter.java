package com.yourorg.platform.foculist.gateway.infrastructure.grpc;

import com.yourorg.platform.foculist.identity.grpc.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class GrpcIdentityClientAdapter {

    @GrpcClient("identity-service")
    private IdentityServiceGrpc.IdentityServiceBlockingStub blockingStub;

    public Mono<ValidationResponse> validateToken(String token) {
        return Mono.fromCallable(() -> {
            ValidationRequest request = ValidationRequest.newBuilder().setToken(token).build();
            return blockingStub.validateToken(request);
        }).onErrorResume(e -> {
            log.error("gRPC Token validation failed", e);
            return Mono.just(ValidationResponse.newBuilder().setValid(false).build());
        });
    }

    public Mono<AuthResponse> authenticate(String tenantId, String email, String password) {
        return Mono.fromCallable(() -> {
            AuthRequest request = AuthRequest.newBuilder()
                    .setTenantId(tenantId)
                    .setEmail(email)
                    .setPassword(password)
                    .build();
            return blockingStub.authenticate(request);
        });
    }
}
