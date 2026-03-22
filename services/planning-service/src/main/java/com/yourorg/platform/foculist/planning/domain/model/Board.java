package com.yourorg.platform.foculist.planning.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record Board(
        UUID id,
        String tenantId,
        UUID projectId,
        String name,
        BoardType type,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy,
        Instant deletedAt,
        Map<String, Object> metadata,
        Long version
) {
    public Board {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be null or blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("type cannot be null");
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

    public Board update(String newName, String updatedBy) {
        return new Board(
                this.id,
                this.tenantId,
                this.projectId,
                newName != null ? newName : this.name,
                this.type,
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
