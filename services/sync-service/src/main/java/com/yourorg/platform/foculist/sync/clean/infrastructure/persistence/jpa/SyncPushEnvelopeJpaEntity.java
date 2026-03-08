package com.yourorg.platform.foculist.sync.clean.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.sync.clean.domain.model.SyncPushEnvelope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "sync_push_envelope",
        indexes = {
                @Index(name = "idx_sync_push_tenant_device_received", columnList = "tenant_id,device_id,received_at")
        }
)
public class SyncPushEnvelopeJpaEntity {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 128)
    private String tenantId;

    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    @Column(name = "payload_version", nullable = false, length = 64)
    private String payloadVersion;

    @Column(name = "pending_changes", nullable = false)
    private int pendingChanges;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "client_sync_time")
    private Instant clientSyncTime;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected SyncPushEnvelopeJpaEntity() {
    }

    SyncPushEnvelopeJpaEntity(
            UUID id,
            String tenantId,
            String deviceId,
            String payloadVersion,
            int pendingChanges,
            String payload,
            Instant clientSyncTime,
            Instant receivedAt,
            long version
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.deviceId = deviceId;
        this.payloadVersion = payloadVersion;
        this.pendingChanges = pendingChanges;
        this.payload = payload;
        this.clientSyncTime = clientSyncTime;
        this.receivedAt = receivedAt;
        this.version = version;
    }

    public static SyncPushEnvelopeJpaEntity fromDomain(SyncPushEnvelope envelope) {
        return new SyncPushEnvelopeJpaEntity(
                envelope.id(),
                envelope.tenantId(),
                envelope.deviceId(),
                envelope.payloadVersion(),
                envelope.pendingChanges(),
                envelope.payload(),
                envelope.clientSyncTime(),
                envelope.receivedAt(),
                envelope.version()
        );
    }

    public SyncPushEnvelope toDomain() {
        return new SyncPushEnvelope(id, tenantId, deviceId, payloadVersion, pendingChanges, payload, clientSyncTime, receivedAt, version);
    }
}
