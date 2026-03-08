package com.yourorg.platform.foculist.sync.clean.domain.model;

import java.time.Instant;
import java.util.UUID;

public record SyncChangeEvent(
        UUID id,
        String tenantId,
        String deviceId,
        SyncChangeType changeType,
        String payload,
        Instant occurredAt,
        long version
) {
    public SyncChangeEvent {
        if (id == null) {
            throw new SyncDomainException("Sync change event id is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new SyncDomainException("Sync change event tenantId is required");
        }
        if (deviceId == null || deviceId.isBlank()) {
            throw new SyncDomainException("Sync change event deviceId is required");
        }
        if (changeType == null) {
            throw new SyncDomainException("Sync change event changeType is required");
        }
        if (payload == null || payload.isBlank()) {
            throw new SyncDomainException("Sync change event payload is required");
        }
        if (occurredAt == null) {
            throw new SyncDomainException("Sync change event occurredAt is required");
        }
        if (version < 0) {
            throw new SyncDomainException("Sync change event version cannot be negative");
        }

        tenantId = tenantId.trim();
        deviceId = deviceId.trim();
        payload = payload.trim();
    }

    public static SyncChangeEvent batch(String tenantId, String deviceId, String payload, Instant occurredAt) {
        return new SyncChangeEvent(
                UUID.randomUUID(),
                tenantId,
                deviceId,
                SyncChangeType.BATCH,
                payload,
                occurredAt == null ? Instant.now() : occurredAt,
                0L
        );
    }
}
