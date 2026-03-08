package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.planning.domain.model.ProjectionCheckpoint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "planning_projection_checkpoint")
public class ProjectionCheckpointJpaEntity {
    @Id
    @Column(name = "projection_name", nullable = false, length = 128)
    private String projectionName;

    @Column(name = "last_occurred_at")
    private Instant lastOccurredAt;

    @Column(name = "last_event_id")
    private UUID lastEventId;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected ProjectionCheckpointJpaEntity() {
    }

    ProjectionCheckpointJpaEntity(String projectionName, Instant lastOccurredAt, UUID lastEventId, Instant updatedAt) {
        this.projectionName = projectionName;
        this.lastOccurredAt = lastOccurredAt;
        this.lastEventId = lastEventId;
        this.updatedAt = updatedAt;
    }

    public static ProjectionCheckpointJpaEntity fromDomain(ProjectionCheckpoint checkpoint) {
        return new ProjectionCheckpointJpaEntity(
                checkpoint.projectionName(),
                checkpoint.lastOccurredAt(),
                checkpoint.lastEventId(),
                checkpoint.updatedAt()
        );
    }

    public ProjectionCheckpoint toDomain() {
        return new ProjectionCheckpoint(projectionName, lastOccurredAt, lastEventId, updatedAt);
    }
}
