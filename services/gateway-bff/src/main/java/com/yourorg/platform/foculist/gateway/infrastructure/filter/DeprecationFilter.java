package com.yourorg.platform.foculist.gateway.infrastructure.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class DeprecationFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            // Signal deprecation for v1 endpoints if the client didn't explicitly request v2
            if (path.contains("/v1/") || !exchange.getRequest().getHeaders().containsKey("X-API-Version")) {
                exchange.getResponse().getHeaders().add("Deprecation", "true");
                exchange.getResponse().getHeaders().add("Link", "<https://docs.foculist.com/api/v1>; rel=\"deprecation\"; type=\"text/html\"");
            }
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
