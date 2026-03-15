package com.yourorg.platform.foculist.gateway.tenancy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@EnableConfigurationProperties(GatewayTenancyProperties.class)
public class GatewayTenantEnforcementFilter implements GlobalFilter, Ordered {
    private final GatewayTenancyProperties properties;
    private final ObjectMapper objectMapper;

    public GatewayTenantEnforcementFilter(
            GatewayTenancyProperties properties,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestPath = exchange.getRequest().getURI().getPath();
        if ((properties.isSkipActuator() && requestPath.startsWith("/actuator")) ||
            requestPath.contains("/v3/api-docs") ||
            requestPath.contains("/swagger-ui")) {
            return chain.filter(exchange);
        }

        return exchange.getPrincipal()
                .filter(principal -> principal instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .flatMap(jwtAuth -> {
                    String tenant = jwtAuth.getToken().getClaimAsString(properties.getJwtClaim());
                    String orgRole = jwtAuth.getToken().getClaimAsString("orgRole");
                    exchange.getAttributes().put("jwt.orgRole", orgRole != null ? orgRole : "GUEST");
                    return Mono.justOrEmpty(tenant);
                })
                .defaultIfEmpty("")
                .flatMap(jwtTenant -> processTenancy(exchange, chain, requestPath, jwtTenant));
    }

    private Mono<Void> processTenancy(ServerWebExchange exchange, GatewayFilterChain chain, String requestPath, String jwtTenantStr) {
        String jwtTenant = StringUtils.hasText(jwtTenantStr) ? jwtTenantStr : null;

        String resolvedTenant = firstNonBlank(
                exchange.getRequest().getHeaders().getFirst(properties.getHeader()),
                exchange.getRequest().getQueryParams().getFirst(properties.getParameter()),
                extractFromPath(requestPath),
                extractFromSubdomain(exchange.getRequest().getURI().getHost()),
                jwtTenant
        );

        if (!StringUtils.hasText(resolvedTenant)) {
            if (properties.isRequired()) {
                return writeError(exchange.getResponse(), HttpStatus.BAD_REQUEST, "Tenant identifier is missing");
            }
            resolvedTenant = properties.getDefaultTenant();
        }

        if (StringUtils.hasText(jwtTenant)
                && properties.isEnforceJwtMatch()
                && !resolvedTenant.equals(jwtTenant)) {
            return writeError(exchange.getResponse(), HttpStatus.FORBIDDEN,
                    "Tenant identifier does not match JWT tenant claim");
        }

        String tenantId = resolvedTenant;
        String orgRole = (String) exchange.getAttributes().getOrDefault("jwt.orgRole", "GUEST");
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate().headers(headers -> {
            headers.remove(properties.getHeader());
            headers.add(properties.getHeader(), tenantId);
            headers.remove("X-User-Role");
            headers.add("X-User-Role", orgRole);
        }).build();

        exchange.getResponse().getHeaders().set(properties.getHeader(), tenantId);

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private String extractFromPath(String requestPath) {
        if (!StringUtils.hasText(properties.getPathPattern())) return null;
        Pattern pattern = Pattern.compile(properties.getPathPattern());
        Matcher matcher = pattern.matcher(requestPath);
        return matcher.matches() ? matcher.group(1) : null;
    }

    private String extractFromSubdomain(String host) {
        if (!StringUtils.hasText(host) || !StringUtils.hasText(properties.getSubdomainPattern())) {
            return null;
        }
        Pattern pattern = Pattern.compile(properties.getSubdomainPattern());
        Matcher matcher = pattern.matcher(host);
        return matcher.matches() ? matcher.group(1) : null;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private Mono<Void> writeError(ServerHttpResponse response, HttpStatus status, String message) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            byte[] body = objectMapper.writeValueAsBytes(Map.of(
                    "error", "TENANCY_RESOLUTION_ERROR",
                    "message", message,
                    "timestamp", Instant.now().toString()
            ));
            return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
        } catch (Exception e) {
            byte[] fallback = ("{\"error\":\"TENANCY_RESOLUTION_ERROR\",\"message\":\"" + message + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(fallback)));
        }
    }
}
