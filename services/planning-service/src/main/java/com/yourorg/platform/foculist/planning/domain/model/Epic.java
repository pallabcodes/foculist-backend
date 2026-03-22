package com.yourorg.platform.foculist.planning.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record Epic(
        UUID id,
        String tenantId,
        UUID projectId,
        String name,
        String summary,
        String color,
        EpicStatus status,
        Instant startDate,
        Instant targetDate,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy,
        Instant deletedAt,
        Map<String, Object> metadata,
        Long version
) {
    public Epic {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be null or blank");
        }
        if (status == null) {
            status = EpicStatus.TO_DO;
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
        if (version == null) {
            version = 0L;
        }
    }

    public Epic update(String newName, String newSummary, String newColor, EpicStatus newStatus, Instant newStartDate, Instant newTargetDate, String updatedBy) {
        return new Epic(
                this.id,
                this.tenantId,
                this.projectId,
                newName != null ? newName : this.name,
                newSummary != null ? newSummary : this.summary,
                newColor != null ? newColor : this.color,
                newStatus != null ? newStatus : this.status,
                newStartDate != null ? newStartDate : this.startDate,
                newTargetDate != null ? newTargetDate : this.targetDate,
                this.createdAt,
                Instant.now(),
                this.createdBy,
                updatedBy,
                this.deletedAt,
                this.metadata,
                this.version
        );
    }
}
