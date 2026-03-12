package com.yourorg.platform.foculist.tenancy;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

public class TenantContextFilter extends OncePerRequestFilter {
    private final TenantResolver tenantResolver;
    private final TenantContextProperties properties;
    private final ObjectMapper objectMapper;

    public TenantContextFilter(TenantResolver tenantResolver, TenantContextProperties properties, ObjectMapper objectMapper) {
        this.tenantResolver = tenantResolver;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return properties.isSkipActuator() && request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String tenantId = tenantResolver.resolve(
                    request.getHeader("Authorization"),
                    request.getHeader(properties.getHeader()),
                    request.getParameter(properties.getParameter()),
                    request.getRequestURI(),
                    request.getServerName()
            );
            TenantContext.set(tenantId);
            MDC.put("tenantId", tenantId);
            response.setHeader(properties.getHeader(), tenantId);
            filterChain.doFilter(request, response);
        } catch (TenantResolutionException e) {
            response.setStatus(e.getStatus());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "error", "TENANCY_RESOLUTION_ERROR",
                    "message", e.getMessage()
            ));
        } finally {
            TenantContext.clear();
            MDC.remove("tenantId");
        }
    }
}
