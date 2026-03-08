package com.yourorg.platform.foculist.project.domain.model;

import java.util.Locale;

public enum ProjectPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static ProjectPriority from(String rawPriority) {
        if (rawPriority == null || rawPriority.isBlank()) {
            return MEDIUM;
        }
        try {
            return valueOf(rawPriority.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ProjectDomainException("Invalid project priority: " + rawPriority);
        }
    }
}
