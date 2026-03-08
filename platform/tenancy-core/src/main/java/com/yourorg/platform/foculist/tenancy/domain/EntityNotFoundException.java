package com.yourorg.platform.foculist.tenancy.domain;

/**
 * Marker for "entity not found" domain errors. Handlers can pattern-match on this
 * to return 404 instead of the default 422.
 */
public abstract class EntityNotFoundException extends DomainException {

    protected EntityNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}
