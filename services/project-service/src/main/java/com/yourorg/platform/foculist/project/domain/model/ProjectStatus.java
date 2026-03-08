package com.yourorg.platform.foculist.project.domain.model;

import java.util.Locale;

public enum ProjectStatus {
    PLANNED,
    RUNNING,
    PAUSED,
    COMPLETED,
    ARCHIVED;

    public static ProjectStatus from(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return RUNNING;
        }
        try {
            return valueOf(rawStatus.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ProjectDomainException("Invalid project status: " + rawStatus);
        }
    }
}
