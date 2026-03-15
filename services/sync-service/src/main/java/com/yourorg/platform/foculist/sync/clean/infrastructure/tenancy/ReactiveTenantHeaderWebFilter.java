package com.yourorg.platform.foculist.sync.clean.infrastructure.tenancy;

import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class ReactiveTenantHeaderWebFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (path.startsWith("/actuator") ||
            path.contains("/v3/api-docs") ||
            path.contains("/swagger-ui")) {
            return chain.filter(exchange);
        }

        String tenantId = firstNonBlank(
                exchange.getRequest().getHeaders().getFirst("X-Tenant-ID"),
                exchange.getRequest().getQueryParams().getFirst("tenant")
        );

        if (tenantId == null) {
            exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
            exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
            byte[] payload = "{\"error\":\"TENANCY_RESOLUTION_ERROR\",\"message\":\"Tenant identifier is missing\"}"
                    .getBytes(StandardCharsets.UTF_8);
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                    .bufferFactory()
                    .wrap(payload)));
        }

        exchange.getResponse().getHeaders().set("X-Tenant-ID", tenantId);
        exchange.getAttributes().put("tenantId", tenantId);
        return chain.filter(exchange);
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return null;
    }
}
