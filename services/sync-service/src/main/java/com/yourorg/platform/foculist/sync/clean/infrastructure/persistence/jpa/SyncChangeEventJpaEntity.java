package com.yourorg.platform.foculist.sync.clean.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.sync.clean.domain.model.SyncChangeEvent;
import com.yourorg.platform.foculist.sync.clean.domain.model.SyncChangeType;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "sync_change_event",
        indexes = {
                @Index(name = "idx_sync_change_tenant_occurred", columnList = "tenant_id,occurred_at")
        }
)
public class SyncChangeEventJpaEntity {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 128)
    private String tenantId;

    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 32)
    private SyncChangeType changeType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected SyncChangeEventJpaEntity() {
    }

    SyncChangeEventJpaEntity(UUID id, String tenantId, String deviceId, SyncChangeType changeType, String payload, Instant occurredAt, long version) {
        this.id = id;
        this.tenantId = tenantId;
        this.deviceId = deviceId;
        this.changeType = changeType;
        this.payload = payload;
        this.occurredAt = occurredAt;
        this.version = version;
    }

    public static SyncChangeEventJpaEntity fromDomain(SyncChangeEvent event) {
        return new SyncChangeEventJpaEntity(
                event.id(),
                event.tenantId(),
                event.deviceId(),
                event.changeType(),
                event.payload(),
                event.occurredAt(),
                event.version()
        );
    }

    public SyncChangeEvent toDomain() {
        return new SyncChangeEvent(id, tenantId, deviceId, changeType, payload, occurredAt, version);
    }
}
