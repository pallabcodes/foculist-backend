package com.yourorg.platform.foculist.tenancy.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<com.yourorg.platform.foculist.tenancy.web.model.ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException ex) {
        String requestId = MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY);
        if (requestId == null) requestId = java.util.UUID.randomUUID().toString();

        var details = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> com.yourorg.platform.foculist.tenancy.web.model.ApiResponse.ApiErrorDetail.builder()
                        .field(err.getField())
                        .reason(err.getCode())
                        .message(err.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());

        var error = com.yourorg.platform.foculist.tenancy.web.model.ApiResponse.ApiError.builder()
                .code("VALIDATION_ERROR")
                .message("Invalid request payload")
                .details(details)
                .build();

        var metadata = com.yourorg.platform.foculist.tenancy.web.model.ApiResponse.ApiMetadata.builder()
                .requestId(requestId)
                .timestamp(java.time.OffsetDateTime.now())
                .build();

        var response = com.yourorg.platform.foculist.tenancy.web.model.ApiResponse.builder()
                .apiVersion("v1")
                .error(error)
                .metadata(metadata)
                .build();

        log.warn("Validation error reqId={} errors={}", requestId, details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<com.yourorg.platform.foculist.tenancy.web.model.ApiResponse<Object>> handleException(Exception ex) {
        String requestId = MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY);
        if (requestId == null) requestId = java.util.UUID.randomUUID().toString();
        log.error("Unhandled exception reqId={}", requestId, ex);

        var error = com.yourorg.platform.foculist.tenancy.web.model.ApiResponse.ApiError.builder()
                .code("INTERNAL_ERROR")
                .message("An unexpected error occurred")
                .build();

        var metadata = com.yourorg.platform.foculist.tenancy.web.model.ApiResponse.ApiMetadata.builder()
                .requestId(requestId)
                .timestamp(java.time.OffsetDateTime.now())
                .build();

        var response = com.yourorg.platform.foculist.tenancy.web.model.ApiResponse.builder()
                .apiVersion("v1")
                .error(error)
                .metadata(metadata)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
