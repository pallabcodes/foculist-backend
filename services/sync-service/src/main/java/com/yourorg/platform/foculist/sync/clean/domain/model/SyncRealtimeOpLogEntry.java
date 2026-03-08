package com.yourorg.platform.foculist.sync.clean.domain.model;

import java.time.Instant;
import java.util.UUID;

public record SyncRealtimeOpLogEntry(
        UUID id,
        String tenantId,
        String projectId,
        String deviceId,
        String destination,
        String payload,
        Instant occurredAt,
        Instant expiresAt
) {
    public SyncRealtimeOpLogEntry {
        if (id == null) {
            throw new SyncDomainException("Realtime op log id is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new SyncDomainException("Realtime op log tenantId is required");
        }
        if (projectId == null || projectId.isBlank()) {
            throw new SyncDomainException("Realtime op log projectId is required");
        }
        if (deviceId == null || deviceId.isBlank()) {
            throw new SyncDomainException("Realtime op log deviceId is required");
        }
        if (destination == null || destination.isBlank()) {
            throw new SyncDomainException("Realtime op log destination is required");
        }
        if (payload == null || payload.isBlank()) {
            throw new SyncDomainException("Realtime op log payload is required");
        }
        if (occurredAt == null || expiresAt == null) {
            throw new SyncDomainException("Realtime op log timestamps are required");
        }
    }

    public static SyncRealtimeOpLogEntry create(
            String tenantId,
            String projectId,
            String deviceId,
            String destination,
            String payload,
            Instant occurredAt,
            Instant expiresAt
    ) {
        return new SyncRealtimeOpLogEntry(
                UUID.randomUUID(),
                tenantId,
                projectId,
                deviceId,
                destination,
                payload,
                occurredAt,
                expiresAt
        );
    }
}
