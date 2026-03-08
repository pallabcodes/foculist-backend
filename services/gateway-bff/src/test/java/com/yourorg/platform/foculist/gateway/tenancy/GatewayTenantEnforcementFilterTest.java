package com.yourorg.platform.foculist.gateway.tenancy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayTenantEnforcementFilterTest {

    @Test
    void rejectsMissingTenantWhenRequired() {
        GatewayTenancyProperties properties = new GatewayTenancyProperties();
        GatewayTenantEnforcementFilter filter = new GatewayTenantEnforcementFilter(
                properties,
                new ObjectMapper()
        );

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/planning/sprints").build()
        );

        GatewayFilterChain chain = serverWebExchange -> Mono.error(new IllegalStateException("should not be called"));

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectsTenantMismatchAgainstJwtClaim() {
        GatewayTenancyProperties properties = new GatewayTenancyProperties();
        GatewayTenantEnforcementFilter filter = new GatewayTenantEnforcementFilter(
                properties,
                new ObjectMapper()
        );

        Jwt jwt = Jwt.withTokenValue("mock")
                .header("alg", "none")
                .claim("tenant", "tenant-b")
                .build();
        JwtAuthenticationToken principal = new JwtAuthenticationToken(jwt);

        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/planning/sprints").header("X-Tenant-ID", "tenant-a").build())
                .mutate()
                .principal(Mono.just(principal))
                .build();

        GatewayFilterChain chain = serverWebExchange -> Mono.error(new IllegalStateException("should not be called"));

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void propagatesResolvedTenantHeaderToDownstreamRequest() {
        GatewayTenancyProperties properties = new GatewayTenancyProperties();
        GatewayTenantEnforcementFilter filter = new GatewayTenantEnforcementFilter(
                properties,
                new ObjectMapper()
        );

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/planning/sprints")
                        .header("X-Tenant-ID", "tenant-a")
                        .build()
        );

        AtomicReference<String> capturedTenant = new AtomicReference<>();
        GatewayFilterChain chain = serverWebExchange -> {
            ServerHttpRequest forwardedRequest = serverWebExchange.getRequest();
            capturedTenant.set(forwardedRequest.getHeaders().getFirst("X-Tenant-ID"));
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(capturedTenant.get()).isEqualTo("tenant-a");
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }
}
