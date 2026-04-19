package com.yourorg.platform.foculist.meeting.clean.domain.model;

import java.time.Instant;
import java.util.UUID;

public record MeetingOutboxEvent(
        UUID id,
        String tenantId,
        String aggregateType,
        UUID aggregateId,
        String eventType,
        String payload,
        MeetingOutboxEventStatus status,
        Instant occurredAt,
        Instant publishedAt,
        int attempts,
        String lastError
) {
    public static MeetingOutboxEvent create(String tenantId, String aggregateType, UUID aggregateId, String eventType, String payload) {
        return new MeetingOutboxEvent(
                UUID.randomUUID(),
                tenantId,
                aggregateType,
                aggregateId,
                eventType,
                payload,
                MeetingOutboxEventStatus.PENDING,
                Instant.now(),
                null,
                0,
                null
        );
    }
}
