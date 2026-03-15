package com.yourorg.platform.foculist.project.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import java.util.Map;

public record Project(
        UUID id,
        String tenantId,
        String name,
        String description,
        ProjectStatus status,
        ProjectPriority priority,
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy,
        Instant deletedAt,
        Map<String, Object> metadata,
        long version
) {

    public Project {
        if (id == null) {
            throw new ProjectDomainException("Project id must be provided");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new ProjectDomainException("Tenant id is required");
        }
        if (name == null || name.isBlank()) {
            throw new ProjectDomainException("Project name is required");
        }
        if (status == null) {
            throw new ProjectDomainException("Project status is required");
        }
        if (priority == null) {
            throw new ProjectDomainException("Project priority is required");
        }
        if (createdAt == null || updatedAt == null) {
            throw new ProjectDomainException("Project timestamps are required");
        }
        if (updatedAt.isBefore(createdAt)) {
            throw new ProjectDomainException("updatedAt cannot be before createdAt");
        }
        if (version < 0) {
            throw new ProjectDomainException("Project version cannot be negative");
        }

        tenantId = tenantId.trim();
        name = name.trim();
        description = normalizeDescription(description);
    }

    public static Project create(
            String tenantId,
            String name,
            String description,
            ProjectStatus status,
            ProjectPriority priority,
            LocalDate dueDate,
            Instant now,
            String createdBy
    ) {
        Instant timestamp = now == null ? Instant.now() : now;
        return new Project(
                UUID.randomUUID(),
                tenantId,
                name,
                description,
                status == null ? ProjectStatus.RUNNING : status,
                priority == null ? ProjectPriority.MEDIUM : priority,
                dueDate,
                timestamp,
                timestamp,
                createdBy,
                null,
                null,
                null,
                0L
        );
    }

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }
}
