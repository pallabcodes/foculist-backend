package com.yourorg.platform.foculist.planning.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Task(
        UUID id,
        String tenantId,
        UUID sprintId,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        Instant createdAt,
        Instant updatedAt,
        Long version
) {
    public Task {
        if (id == null) {
            throw new PlanningDomainException("Task id is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new PlanningDomainException("Tenant id is required");
        }
        if (title == null || title.isBlank()) {
            throw new PlanningDomainException("Task title is required");
        }
        if (status == null) {
            throw new PlanningDomainException("Task status is required");
        }
        if (priority == null) {
            throw new PlanningDomainException("Task priority is required");
        }
        if (createdAt == null || updatedAt == null) {
            throw new PlanningDomainException("Task timestamps are required");
        }
    }

    public static Task create(
            String tenantId,
            UUID sprintId,
            String title,
            String description,
            TaskStatus status,
            TaskPriority priority,
            Instant now
    ) {
        return new Task(
                UUID.randomUUID(),
                tenantId,
                sprintId,
                title.trim(),
                description,
                status,
                priority,
                now,
                now,
                0L
        );
    }

    public Task update(UUID sprintId, String title, String description, TaskPriority priority, Instant now) {
        return new Task(id, tenantId, sprintId, title.trim(), description, status, priority, createdAt, now, version);
    }

    public Task updateStatus(TaskStatus newStatus, Instant now) {
        return new Task(id, tenantId, sprintId, title, description, newStatus, priority, createdAt, now, version);
    }
}
