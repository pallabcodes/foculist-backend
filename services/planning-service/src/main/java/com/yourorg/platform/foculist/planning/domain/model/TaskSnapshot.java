package com.yourorg.platform.foculist.planning.domain.model;

import java.time.Instant;
import java.util.UUID;

import java.util.Map;

public record TaskSnapshot(
        UUID id,
        String tenantId,
        UUID taskId,
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
        long version,
        Instant snapshottedAt
) {
    public TaskSnapshot {
        if (id == null) {
            throw new PlanningDomainException("Snapshot id is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new PlanningDomainException("Snapshot tenant id is required");
        }
        if (taskId == null) {
            throw new PlanningDomainException("Snapshot task id is required");
        }
        if (title == null || title.isBlank()) {
            throw new PlanningDomainException("Snapshot title is required");
        }
        if (status == null) {
            throw new PlanningDomainException("Snapshot status is required");
        }
        if (priority == null) {
            throw new PlanningDomainException("Snapshot priority is required");
        }
        if (createdAt == null || updatedAt == null || snapshottedAt == null) {
            throw new PlanningDomainException("Snapshot timestamps are required");
        }
    }

    public static TaskSnapshot fromTask(Task task, Instant snapshottedAt) {
        return new TaskSnapshot(
                UUID.randomUUID(),
                task.tenantId(),
                task.id(),
                task.sprintId(),
                task.epicId(),
                task.boardColumnId(),
                task.title(),
                task.description(),
                task.status(),
                task.priority(),
                task.createdAt(),
                task.updatedAt(),
                task.createdBy(),
                task.updatedBy(),
                task.deletedAt(),
                task.metadata(),
                task.version() == null ? 0L : task.version(),
                snapshottedAt
        );
    }

    public Task toTask() {
        return new Task(
                taskId,
                tenantId,
                sprintId,
                epicId,
                boardColumnId,
                title,
                description,
                status,
                priority,
                createdAt,
                updatedAt,
                createdBy,
                updatedBy,
                deletedAt,
                metadata,
                version
        );
    }
}
