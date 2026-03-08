package com.yourorg.platform.foculist.planning.web;

import java.util.List;

public record ApiErrorResponse(
        String requestId,
        String status,
        List<ApiResponse.ApiError> errors
) {
    public static ApiErrorResponse of(String requestId, List<ApiResponse.ApiError> errors) {
        return new ApiErrorResponse(requestId, "ERROR", errors);
    }
}
