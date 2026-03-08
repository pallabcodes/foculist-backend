package com.yourorg.platform.foculist.project.domain.model;

import com.yourorg.platform.foculist.tenancy.domain.DomainException;

public class ProjectDomainException extends DomainException {
    public ProjectDomainException(String message) {
        super("PROJECT_DOMAIN_ERROR", message);
    }
}
