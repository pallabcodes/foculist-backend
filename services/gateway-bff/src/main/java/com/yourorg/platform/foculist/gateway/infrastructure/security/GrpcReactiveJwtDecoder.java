package com.yourorg.platform.foculist.gateway.infrastructure.security;

import com.yourorg.platform.foculist.gateway.infrastructure.grpc.GrpcIdentityClientAdapter;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class GrpcReactiveJwtDecoder implements ReactiveJwtDecoder {

    private final GrpcIdentityClientAdapter identityClientAdapter;

    @Override
    public Mono<Jwt> decode(String token) throws JwtException {
        return identityClientAdapter.validateToken(token)
                .flatMap(response -> {
                    if (!response.getValid()) {
                        return Mono.error(new JwtException("Invalid token (validated via gRPC)"));
                    }

                    Map<String, Object> claims = new HashMap<>();
                    claims.put("sub", response.getEmail());
                    claims.put("userId", response.getUserId());
                    claims.put("tenant", response.getTenantId());
                    claims.put("roles", response.getRolesList());

                    // Standard JWT headers
                    Map<String, Object> headers = new HashMap<>();
                    headers.put("alg", "HmacSHA256"); // Theoretical since we validated via RPC

                    Jwt jwt = new Jwt(
                            token,
                            Instant.now(),
                            Instant.now().plusSeconds(3600), // Mocked for now
                            headers,
                            claims);
                    return Mono.just(jwt);
                });
    }
}
