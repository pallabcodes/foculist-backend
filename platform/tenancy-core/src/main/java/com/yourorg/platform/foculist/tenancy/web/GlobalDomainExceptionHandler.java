package com.yourorg.platform.foculist.tenancy.web;

import com.yourorg.platform.foculist.tenancy.domain.DomainException;
import com.yourorg.platform.foculist.tenancy.domain.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Unified exception handler for the entire DomainException hierarchy.
 *
 * <p>Uses Java 21 pattern matching to exhaustively distinguish between
 * {@link EntityNotFoundException} (404) and generic {@link DomainException} (422)
 * at the tenancy-core level, eliminating the need for per-service boilerplate handlers.
 *
 * <p>Per-service handlers can still exist for truly service-specific concerns,
 * but the common path is covered here.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE - 10) // Runs after service-specific handlers
public class GlobalDomainExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalDomainExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiErrorResponse> handleDomainException(DomainException ex, HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-ID");

        log.warn("Domain exception [code={}, requestId={}]: {}", ex.errorCode(), requestId, ex.getMessage());

        return switch (ex) {
            case EntityNotFoundException notFound -> ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiErrorResponse.of(requestId, notFound.errorCode(), notFound.getMessage()));
            default -> ResponseEntity
                    .status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(ApiErrorResponse.of(requestId, ex.errorCode(), ex.getMessage()));
        };
    }
}
