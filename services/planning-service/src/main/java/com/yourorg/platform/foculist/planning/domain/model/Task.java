package com.yourorg.platform.foculist.planning.domain.model;

import java.time.Instant;
import java.util.UUID;

import java.util.Map;

public record Task(
        UUID id,
        String tenantId,
        UUID sprintId,
        UUID epicId,
        UUID boardColumnId,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy,
        Instant deletedAt,
        Map<String, Object> metadata,
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
            UUID epicId,
            UUID boardColumnId,
            String title,
            String description,
            TaskStatus status,
            TaskPriority priority,
            Instant now,
            String createdBy
    ) {
        return new Task(
                UUID.randomUUID(),
                tenantId,
                sprintId,
                epicId,
                boardColumnId,
                title.trim(),
                description,
                status,
                priority,
                now,
                now,
                createdBy,
                null,
                null,
                null,
                0L
        );
    }

    public Task update(UUID sprintId, UUID epicId, UUID boardColumnId, String title, String description, TaskPriority priority, Instant now, String updatedBy) {
        return new Task(id, tenantId, sprintId, epicId, boardColumnId, title.trim(), description, status, priority, createdAt, now, createdBy, updatedBy, deletedAt, metadata, version);
    }

    public Task updateStatus(TaskStatus newStatus, Instant now, String updatedBy) {
        return new Task(id, tenantId, sprintId, epicId, boardColumnId, title, description, newStatus, priority, createdAt, now, createdBy, updatedBy, deletedAt, metadata, version);
    }
}
