package com.yourorg.platform.foculist.planning.domain.model;

public enum TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static TaskPriority from(String raw) {
        if (raw == null || raw.isBlank()) {
            return MEDIUM;
        }
        try {
            return TaskPriority.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new PlanningDomainException("Invalid task priority: " + raw);
        }
    }
}
