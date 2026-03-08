package com.yourorg.platform.foculist.planning.domain.model;

import java.time.Instant;
import java.util.UUID;

public record TaskProjection(
        UUID id,
        String tenantId,
        UUID sprintId,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public TaskProjection {
        if (id == null) {
            throw new PlanningDomainException("Task projection id is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new PlanningDomainException("Task projection tenant id is required");
        }
        if (title == null || title.isBlank()) {
            throw new PlanningDomainException("Task projection title is required");
        }
        if (status == null) {
            throw new PlanningDomainException("Task projection status is required");
        }
        if (priority == null) {
            throw new PlanningDomainException("Task projection priority is required");
        }
        if (createdAt == null || updatedAt == null) {
            throw new PlanningDomainException("Task projection timestamps are required");
        }
    }

    public static TaskProjection fromTask(Task task) {
        return new TaskProjection(
                task.id(),
                task.tenantId(),
                task.sprintId(),
                task.title(),
                task.description(),
                task.status(),
                task.priority(),
                task.createdAt(),
                task.updatedAt(),
                task.version() == null ? 0L : task.version()
        );
    }
}
