package com.yourorg.platform.foculist.gateway.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final GrpcReactiveJwtDecoder grpcReactiveJwtDecoder;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(csrf -> csrf.disable()) // Stateless API
            .authorizeExchange(exchanges -> exchanges
                // Permit actuation, version discovery, swagger, and local Auth
                .pathMatchers("/actuator/**", "/api/versions", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**", "/webjars/swagger-ui/**", "/api/*/v3/api-docs/**", "/api/identity/v1/auth/**", "/api/identity/v1/users/register").permitAll()
                // All other gateway routes require a valid JWT
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtDecoder(grpcReactiveJwtDecoder))
            );

        return http.build();
    }

}
