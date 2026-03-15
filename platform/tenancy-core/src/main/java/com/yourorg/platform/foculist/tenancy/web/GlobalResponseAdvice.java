package com.yourorg.platform.foculist.tenancy.web;

import com.yourorg.platform.foculist.tenancy.web.model.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Value("${spring.application.name:foculist-service}")
    private String serviceName;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // Skip wrapping if it's already an ApiResponse or a raw String (to avoid ClassCastException in some converters)
        Class<?> parameterType = returnType.getParameterType();
        if (ApiResponse.class.isAssignableFrom(parameterType) || String.class.isAssignableFrom(parameterType)) {
            return false;
        }

        // Skip documentation paths
        org.springframework.web.context.request.ServletRequestAttributes attributes = 
            (org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            String path = attributes.getRequest().getRequestURI();
            if (path.contains("/v3/api-docs") || path.contains("/swagger-ui")) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        String requestId = "N/A";
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest req = servletRequest.getServletRequest();
            requestId = (String) req.getAttribute(RequestIdFilter.REQUEST_ID_MDC_KEY);
            if (requestId == null) {
                requestId = UUID.randomUUID().toString(); // Fallback
            }
        }

        ApiResponse.ApiMetadata metadata = ApiResponse.ApiMetadata.builder()
                .requestId(requestId)
                .timestamp(OffsetDateTime.now())
                .environment(activeProfile)
                .serviceName(serviceName)
                .build();

        // Environment-driven metadata policy: Stack traces and internal info only in non-prod
        if (!activeProfile.toLowerCase().contains("prod")) {
            // Placeholder for trace ID if we had Sleuth/Brave/OTel tracer bean access here
            metadata.setTraceId("TODO-Trace-ID");
        }

        return ApiResponse.builder()
                .apiVersion("v1")
                .data(body)
                .metadata(metadata)
                .build();
    }
}
