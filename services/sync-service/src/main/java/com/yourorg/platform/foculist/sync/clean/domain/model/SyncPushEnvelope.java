package com.yourorg.platform.foculist.sync.clean.domain.model;

import java.time.Instant;
import java.util.UUID;

public record SyncPushEnvelope(
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
    public SyncPushEnvelope {
        if (id == null) {
            throw new SyncDomainException("Sync push envelope id is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new SyncDomainException("Sync push envelope tenantId is required");
        }
        if (deviceId == null || deviceId.isBlank()) {
            throw new SyncDomainException("Sync push envelope deviceId is required");
        }
        if (payloadVersion == null || payloadVersion.isBlank()) {
            throw new SyncDomainException("Sync push envelope payloadVersion is required");
        }
        if (pendingChanges < 0) {
            throw new SyncDomainException("pendingChanges cannot be negative");
        }
        if (pendingChanges > 100_000) {
            throw new SyncDomainException("pendingChanges is too large");
        }
        if (payload == null || payload.isBlank()) {
            throw new SyncDomainException("Sync push envelope payload is required");
        }
        if (receivedAt == null) {
            throw new SyncDomainException("Sync push envelope receivedAt is required");
        }
        if (version < 0) {
            throw new SyncDomainException("Sync push envelope version cannot be negative");
        }

        tenantId = tenantId.trim();
        deviceId = deviceId.trim();
        payloadVersion = payloadVersion.trim();
        payload = payload.trim();
    }

    public static SyncPushEnvelope create(
            String tenantId,
            String deviceId,
            String payloadVersion,
            int pendingChanges,
            String payload,
            Instant clientSyncTime,
            Instant receivedAt
    ) {
        Instant now = receivedAt == null ? Instant.now() : receivedAt;
        return new SyncPushEnvelope(
                UUID.randomUUID(),
                tenantId,
                deviceId,
                payloadVersion,
                pendingChanges,
                payload,
                clientSyncTime,
                now,
                0L
        );
    }
}
