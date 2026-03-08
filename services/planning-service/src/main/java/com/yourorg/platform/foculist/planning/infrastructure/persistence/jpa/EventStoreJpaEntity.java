package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.planning.domain.model.EventStore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "planning_events",
        indexes = {
                @Index(name = "idx_planning_events_aggregate", columnList = "aggregate_id, aggregate_type"),
                @Index(name = "idx_planning_events_tenant_aggregate", columnList = "tenant_id, aggregate_id, aggregate_type")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_planning_events_aggregate_version", columnNames = {"aggregate_id", "aggregate_type", "version"})
        }
)
public class EventStoreJpaEntity {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 128)
    private String tenantId;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "aggregate_type", nullable = false, length = 64)
    private String aggregateType;

    @Column(name = "event_type", nullable = false, length = 128)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected EventStoreJpaEntity() {}

    EventStoreJpaEntity(UUID id, String tenantId, UUID aggregateId, String aggregateType, String eventType, String payload, long version, Instant occurredAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.payload = payload;
        this.version = version;
        this.occurredAt = occurredAt;
    }

    public static EventStoreJpaEntity fromDomain(EventStore event) {
        return new EventStoreJpaEntity(
                event.id(),
                event.tenantId(),
                event.aggregateId(),
                event.aggregateType(),
                event.eventType(),
                event.payload(),
                event.version(),
                event.occurredAt()
        );
    }

    public EventStore toDomain() {
        return new EventStore(id, tenantId, aggregateId, aggregateType, eventType, payload, version, occurredAt);
    }
}
