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
    public boolean supports(
            @org.springframework.lang.NonNull MethodParameter returnType,
            @org.springframework.lang.NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        // Skip wrapping if it's already an ApiResponse
        if (ApiResponse.class.isAssignableFrom(returnType.getParameterType())) {
            return false;
        }

        // CRITICAL: Skip wrapping if the converter is StringHttpMessageConverter
        // This avoids ClassCastException when Spring expects a raw String for the body
        if (org.springframework.http.converter.StringHttpMessageConverter.class.isAssignableFrom(converterType)) {
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
    public Object beforeBodyWrite(
            @org.springframework.lang.Nullable Object body,
            @org.springframework.lang.NonNull MethodParameter returnType,
            @org.springframework.lang.NonNull MediaType selectedContentType,
            @org.springframework.lang.NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
            @org.springframework.lang.NonNull ServerHttpRequest request,
            @org.springframework.lang.NonNull ServerHttpResponse response) {

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

        // Handle Spring Boot's DefaultErrorAttributes map explicitly
        if (body instanceof java.util.Map mapBody) {
            if (mapBody.containsKey("status") && mapBody.containsKey("error")) {
                Integer status = (Integer) mapBody.get("status");
                String errorType = (String) mapBody.get("error");
                String message = (String) mapBody.get("message");
                String path = (String) mapBody.get("path");
                
                java.util.List<ApiResponse.ApiErrorDetail> errorDetails = new java.util.ArrayList<>();
                
                // Extract Spring validation errors if explicitly exposed
                if (mapBody.containsKey("errors") && mapBody.get("errors") instanceof java.util.List) {
                    java.util.List<?> validationErrors = (java.util.List<?>) mapBody.get("errors");
                    for (Object err : validationErrors) {
                        if (err instanceof org.springframework.validation.FieldError ferr) {
                            errorDetails.add(ApiResponse.ApiErrorDetail.builder()
                                    .field(ferr.getField())
                                    .reason(ferr.getCode())
                                    .message(ferr.getDefaultMessage())
                                    .build());
                        } else if (err instanceof java.util.Map verr) {
                            // Fallback for serialized maps
                            errorDetails.add(ApiResponse.ApiErrorDetail.builder()
                                    .field((String) verr.get("field"))
                                    .reason((String) verr.get("code"))
                                    .message((String) verr.get("defaultMessage"))
                                    .build());
                        }
                    }
                }
                
                // Fallback to path if no validation fields were extracted
                if (errorDetails.isEmpty() && path != null) {
                    errorDetails.add(ApiResponse.ApiErrorDetail.builder()
                            .field("path")
                            .message(path)
                            .build());
                }

                // Sanitize internal Spring exception messages
                String finalMessage = message != null && !message.isBlank() ? message : errorType;
                if (finalMessage.startsWith("Validation failed for object")) {
                    finalMessage = "Invalid request parameters";
                }

                ApiResponse.ApiError error = ApiResponse.ApiError.builder()
                        .code("HTTP_" + status)
                        .message(finalMessage)
                        .details(errorDetails)
                        .build();

                return ApiResponse.builder()
                        .apiVersion("v1")
                        .error(error)
                        .metadata(metadata)
                        .build();
            }
        }

        return ApiResponse.builder()
                .apiVersion("v1")
                .data(body)
                .metadata(metadata)
                .build();
    }
}
