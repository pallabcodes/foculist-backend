package com.yourorg.platform.foculist.gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.regex.Pattern;

@Component
public class WafSimulationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(WafSimulationFilter.class);

    // Simple SQLi and XSS patterns for local simulation
    private static final Pattern SQLI_PATTERN = Pattern.compile("('.+--)|(--)|(truncate)|(drop)|(union)|(select.*from)", Pattern.CASE_INSENSITIVE);
    private static final Pattern XSS_PATTERN = Pattern.compile("(<script.*?>)|(javascript:)|(onload=)", Pattern.CASE_INSENSITIVE);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String query = exchange.getRequest().getQueryParams().toString();

        if (isMalicious(path) || isMalicious(query)) {
            logger.warn("🛑 BLOCKING MALICIOUS REQUEST: Path={}, Query={}", path, query);
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    private boolean isMalicious(String input) {
        if (input == null) return false;
        return SQLI_PATTERN.matcher(input).find() || XSS_PATTERN.matcher(input).find();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
