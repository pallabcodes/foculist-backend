package com.yourorg.platform.foculist.resource.clean.domain.model;

import com.yourorg.platform.foculist.tenancy.domain.DomainException;

public final class ResourceDomainException extends DomainException {
    public ResourceDomainException(String message) {
        super("RESOURCE_DOMAIN_ERROR", message);
    }
}
