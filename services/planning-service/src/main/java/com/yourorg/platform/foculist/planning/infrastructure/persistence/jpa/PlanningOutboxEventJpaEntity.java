package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.planning.domain.model.OutboxEvent;
import com.yourorg.platform.foculist.planning.domain.model.OutboxEventStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "planning_outbox_event",
        indexes = {
                @Index(name = "idx_planning_outbox_status_occurred", columnList = "status,occurred_at"),
                @Index(name = "idx_planning_outbox_tenant", columnList = "tenant_id"),
                @Index(name = "idx_planning_outbox_aggregate", columnList = "aggregate_type,aggregate_id")
        }
)
public class PlanningOutboxEventJpaEntity {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 128)
    private String tenantId;

    @Column(name = "aggregate_type", nullable = false, length = 128)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OutboxEventStatus status;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_error")
    private String lastError;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected PlanningOutboxEventJpaEntity() {
    }

    PlanningOutboxEventJpaEntity(
            UUID id,
            String tenantId,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            String payload,
            OutboxEventStatus status,
            Instant occurredAt,
            Instant publishedAt,
            int attempts,
            String lastError,
            long version
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.status = status;
        this.occurredAt = occurredAt;
        this.publishedAt = publishedAt;
        this.attempts = attempts;
        this.lastError = lastError;
        this.version = version;
    }

    public static PlanningOutboxEventJpaEntity fromDomain(OutboxEvent event) {
        return new PlanningOutboxEventJpaEntity(
                event.id(),
                event.tenantId(),
                event.aggregateType(),
                event.aggregateId(),
                event.eventType(),
                event.payload(),
                event.status(),
                event.occurredAt(),
                event.publishedAt(),
                event.attempts(),
                event.lastError(),
                0L
        );
    }

    public OutboxEvent toDomain() {
        return new OutboxEvent(
                id,
                tenantId,
                aggregateType,
                aggregateId,
                eventType,
                payload,
                status,
                occurredAt,
                publishedAt,
                attempts,
                lastError
        );
    }

    public UUID getId() {
        return id;
    }

    public OutboxEventStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public void markProcessing() {
        this.status = OutboxEventStatus.PROCESSING;
    }

    public void markPublished(Instant publishedAt) {
        this.status = OutboxEventStatus.PUBLISHED;
        this.publishedAt = publishedAt;
        this.lastError = null;
    }

    public void markFailed(String lastError) {
        this.status = OutboxEventStatus.FAILED;
        this.lastError = lastError;
        this.attempts = this.attempts + 1;
    }
}
