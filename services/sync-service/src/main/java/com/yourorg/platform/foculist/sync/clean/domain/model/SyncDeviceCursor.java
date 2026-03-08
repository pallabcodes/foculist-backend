package com.yourorg.platform.foculist.sync.clean.domain.model;

import java.time.Instant;
import java.util.UUID;

public record SyncDeviceCursor(
        UUID id,
        String tenantId,
        String deviceId,
        Instant lastClientSync,
        Instant lastPullAt,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public SyncDeviceCursor {
        if (id == null) {
            throw new SyncDomainException("Sync device cursor id is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new SyncDomainException("Sync device cursor tenantId is required");
        }
        if (deviceId == null || deviceId.isBlank()) {
            throw new SyncDomainException("Sync device cursor deviceId is required");
        }
        if (createdAt == null || updatedAt == null) {
            throw new SyncDomainException("Sync device cursor timestamps are required");
        }
        if (updatedAt.isBefore(createdAt)) {
            throw new SyncDomainException("Sync device cursor updatedAt cannot be before createdAt");
        }
        if (version < 0) {
            throw new SyncDomainException("Sync device cursor version cannot be negative");
        }
        tenantId = tenantId.trim();
        deviceId = deviceId.trim();
    }

    public static SyncDeviceCursor create(
            String tenantId,
            String deviceId,
            Instant lastClientSync,
            Instant lastPullAt
    ) {
        Instant now = lastPullAt == null ? Instant.now() : lastPullAt;
        return new SyncDeviceCursor(
                UUID.randomUUID(),
                tenantId,
                deviceId,
                lastClientSync,
                lastPullAt,
                now,
                now,
                0L
        );
    }

    public SyncDeviceCursor touch(Instant lastClientSync, Instant pulledAt) {
        Instant now = pulledAt == null ? Instant.now() : pulledAt;
        Instant resolvedLastClientSync = lastClientSync == null ? this.lastClientSync : lastClientSync;
        return new SyncDeviceCursor(
                id,
                tenantId,
                deviceId,
                resolvedLastClientSync,
                now,
                createdAt,
                now,
                version
        );
    }
}
