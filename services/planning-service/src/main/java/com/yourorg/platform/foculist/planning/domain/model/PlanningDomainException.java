package com.yourorg.platform.foculist.planning.domain.model;

import com.yourorg.platform.foculist.tenancy.domain.DomainException;

public final class PlanningDomainException extends DomainException {
    public PlanningDomainException(String message) {
        super("PLANNING_DOMAIN_ERROR", message);
    }
}
