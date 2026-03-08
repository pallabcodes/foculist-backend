package com.yourorg.platform.foculist.project.domain.model;

import com.yourorg.platform.foculist.tenancy.domain.EntityNotFoundException;

public final class ProjectNotFoundException extends EntityNotFoundException {
    public ProjectNotFoundException(String message) {
        super("PROJECT_NOT_FOUND", message);
    }
}
