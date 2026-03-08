package com.yourorg.platform.foculist.sync.clean.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.sync.clean.domain.model.SyncDeviceCursor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "sync_device_cursor",
        indexes = {
                @Index(name = "idx_sync_cursor_tenant_device", columnList = "tenant_id,device_id", unique = true)
        }
)
public class SyncDeviceCursorJpaEntity {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 128)
    private String tenantId;

    @Column(name = "device_id", nullable = false, length = 128)
    private String deviceId;

    @Column(name = "last_client_sync")
    private Instant lastClientSync;

    @Column(name = "last_pull_at")
    private Instant lastPullAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected SyncDeviceCursorJpaEntity() {
    }

    SyncDeviceCursorJpaEntity(
            UUID id,
            String tenantId,
            String deviceId,
            Instant lastClientSync,
            Instant lastPullAt,
            Instant createdAt,
            Instant updatedAt,
            long version
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.deviceId = deviceId;
        this.lastClientSync = lastClientSync;
        this.lastPullAt = lastPullAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static SyncDeviceCursorJpaEntity fromDomain(SyncDeviceCursor cursor) {
        return new SyncDeviceCursorJpaEntity(
                cursor.id(),
                cursor.tenantId(),
                cursor.deviceId(),
                cursor.lastClientSync(),
                cursor.lastPullAt(),
                cursor.createdAt(),
                cursor.updatedAt(),
                cursor.version()
        );
    }

    public SyncDeviceCursor toDomain() {
        return new SyncDeviceCursor(id, tenantId, deviceId, lastClientSync, lastPullAt, createdAt, updatedAt, version);
    }
}
