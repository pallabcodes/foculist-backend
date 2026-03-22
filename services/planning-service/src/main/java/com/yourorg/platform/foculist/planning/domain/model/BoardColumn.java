package com.yourorg.platform.foculist.planning.domain.model;

import java.time.Instant;
import java.util.UUID;

public record BoardColumn(
        UUID id,
        String tenantId,
        UUID boardId,
        String name,
        String statusMapping,
        int orderIndex,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy,
        Instant deletedAt,
        Long version
) {
    public BoardColumn {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId cannot be null or blank");
        }
        if (boardId == null) {
            throw new IllegalArgumentException("boardId cannot be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be null or blank");
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

    public BoardColumn update(String newName, String newStatusMapping, Integer newOrderIndex, String updatedBy) {
        return new BoardColumn(
                this.id,
                this.tenantId,
                this.boardId,
                newName != null ? newName : this.name,
                newStatusMapping != null ? newStatusMapping : this.statusMapping,
                newOrderIndex != null ? newOrderIndex : this.orderIndex,
                this.createdAt,
                Instant.now(),
                this.createdBy,
                updatedBy,
                this.deletedAt,
                this.version
        );
    }
}
