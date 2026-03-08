package com.yourorg.platform.foculist.gateway.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(csrf -> csrf.disable()) // Stateless API
            .authorizeExchange(exchanges -> exchanges
                // Permit actuation and local Auth loops
                .pathMatchers("/actuator/**", "/api/identity/v1/auth/**", "/api/identity/v1/users/register").permitAll()
                // All other gateway routes require a valid JWT
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {}) // Relies on properties (spring.security.oauth2.resourceserver.jwt.issuer-uri)
            );

        return http.build();
    }
}
