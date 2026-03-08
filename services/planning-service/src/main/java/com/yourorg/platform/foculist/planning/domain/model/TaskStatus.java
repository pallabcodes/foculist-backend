package com.yourorg.platform.foculist.planning.domain.model;

public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    REVIEW,
    TESTING,
    DONE;

    public static TaskStatus from(String raw) {
        if (raw == null || raw.isBlank()) {
            return TODO;
        }
        try {
            return TaskStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new PlanningDomainException("Invalid task status: " + raw);
        }
    }
}
