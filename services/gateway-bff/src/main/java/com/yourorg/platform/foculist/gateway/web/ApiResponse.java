package com.yourorg.platform.foculist.gateway.web;

import java.util.List;

public record ApiResponse<T>(
        String requestId,
        String status,
        T data,
        Meta meta,
        List<ApiError> errors
) {
    public static <T> ApiResponse<T> success(String requestId, T data, Meta meta) {
        return new ApiResponse<>(requestId, "OK", data, meta, List.of());
    }

    public record Meta(Integer page, Integer size, String nextPageCursor, Boolean hasMore) {}
    public record ApiError(String code, String message, String field) {}
}
