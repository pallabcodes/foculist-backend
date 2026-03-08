package com.yourorg.platform.foculist.planning.domain.model;

public enum SprintStatus {
    PLANNED,
    ACTIVE,
    CLOSED;

    public static SprintStatus from(String raw) {
        if (raw == null || raw.isBlank()) {
            return PLANNED;
        }
        try {
            return SprintStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new PlanningDomainException("Invalid sprint status: " + raw);
        }
    }
}
