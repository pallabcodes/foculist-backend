package com.yourorg.platform.foculist.tenancy.web;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        String requestId,
        String error,
        String message,
        List<String> details,
        Instant timestamp
) {
    public static ApiErrorResponse of(String requestId, String error, String message, List<String> details) {
        return new ApiErrorResponse(requestId, error, message, details, Instant.now());
    }

    public static ApiErrorResponse of(String requestId, String error, String message) {
        return new ApiErrorResponse(requestId, error, message, null, Instant.now());
    }
}
