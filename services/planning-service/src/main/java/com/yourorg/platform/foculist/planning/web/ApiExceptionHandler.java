package com.yourorg.platform.foculist.planning.web;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String requestId = currentRequestId();
        List<ApiResponse.ApiError> errors = ex.getBindingResult().getAllErrors().stream()
                .map(err -> new ApiResponse.ApiError(
                        "VALIDATION_ERROR",
                        err.getDefaultMessage(),
                        err instanceof FieldError fe ? fe.getField() : null
                ))
                .toList();
        return ResponseEntity.badRequest().body(ApiErrorResponse.of(requestId, errors));
    }

    @ExceptionHandler(UnknownVersionException.class)
    @ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
    public ResponseEntity<ApiErrorResponse> handleUnknownVersion(UnknownVersionException ex) {
        String requestId = currentRequestId();
        ApiResponse.ApiError error = new ApiResponse.ApiError("UNSUPPORTED_VERSION", ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(ApiErrorResponse.of(requestId, List.of(error)));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleStatusException(ResponseStatusException ex) {
        String requestId = currentRequestId();
        ApiResponse.ApiError error = new ApiResponse.ApiError(ex.getStatusCode().toString(), ex.getReason(), null);
        return ResponseEntity.status(ex.getStatusCode()).body(ApiErrorResponse.of(requestId, List.of(error)));
    }

    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ApiErrorResponse> handleOptimisticLocking(org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
        String requestId = currentRequestId();
        ApiResponse.ApiError error = new ApiResponse.ApiError(
                "CONFLICT_ERROR",
                "This resource was modified by another user. Please refresh and try again.",
                null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiErrorResponse.of(requestId, List.of(error)));
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiErrorResponse> handleGeneric(RuntimeException ex) {
        String requestId = currentRequestId();
        ApiResponse.ApiError error = new ApiResponse.ApiError("INTERNAL_ERROR", "Unexpected error", null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiErrorResponse.of(requestId, List.of(error)));
    }

    private String currentRequestId() {
        Object attr = org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()
                .getAttribute(com.yourorg.platform.foculist.tenancy.web.RequestIdFilter.REQUEST_ID_MDC_KEY, 0);
        if (attr instanceof String s && !s.isBlank()) {
            return s;
        }
        return java.util.UUID.randomUUID().toString();
    }
}
