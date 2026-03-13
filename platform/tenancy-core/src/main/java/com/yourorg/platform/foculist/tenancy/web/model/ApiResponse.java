package com.yourorg.platform.foculist.tenancy.web.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private String apiVersion;
    private T data;
    private ApiError error;
    private ApiMetadata metadata;

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApiError {
        private String code;
        private String message;
        private List<ApiErrorDetail> details;
    }

    @Data
    @Builder
    public static class ApiErrorDetail {
        private String field;
        private String reason;
        private String message;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApiMetadata {
        private String requestId;
        private String traceId;
        private OffsetDateTime timestamp;
        private Long processingTimeMs;
        private String environment;
        private String serviceName;
        private String stackTrace; // Only in dev/staging
    }
}
