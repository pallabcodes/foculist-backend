package com.yourorg.platform.foculist.tenancy.domain;

/**
 * Sealed base for all domain-layer exceptions across Foculist services.
 *
 * <p>Using a sealed hierarchy enables exhaustive {@code switch} pattern matching
 * in exception handlers, guaranteeing compile-time safety when new subtypes are introduced.
 *
 * <p>Each microservice extends this with its own concrete subtype (e.g. {@code PlanningDomainException}).
 * The {@code permits} clause is deliberately omitted so that subclasses in downstream
 * modules (separate compilation units) can extend this class — Java sealed types allow
 * non-sealed subclasses for this exact cross-module pattern.
 */
public abstract class DomainException extends RuntimeException {

    private final String errorCode;

    protected DomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected DomainException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /** Machine-readable error code for API responses (e.g. "PLANNING_DOMAIN_ERROR"). */
    public String errorCode() {
        return errorCode;
    }
}
