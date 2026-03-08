package com.yourorg.platform.foculist.planning.domain.model;

import java.time.Instant;
import java.util.UUID;

public record OutboxEvent(
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
        String lastError
) {
    public OutboxEvent {
        if (id == null) {
            throw new PlanningDomainException("Outbox event id is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new PlanningDomainException("Outbox tenant id is required");
        }
        if (aggregateType == null || aggregateType.isBlank()) {
            throw new PlanningDomainException("Outbox aggregate type is required");
        }
        if (aggregateId == null) {
            throw new PlanningDomainException("Outbox aggregate id is required");
        }
        if (eventType == null || eventType.isBlank()) {
            throw new PlanningDomainException("Outbox event type is required");
        }
        if (payload == null || payload.isBlank()) {
            throw new PlanningDomainException("Outbox payload is required");
        }
        if (status == null) {
            throw new PlanningDomainException("Outbox status is required");
        }
        if (occurredAt == null) {
            throw new PlanningDomainException("Outbox occurredAt is required");
        }
        if (attempts < 0) {
            throw new PlanningDomainException("Outbox attempts cannot be negative");
        }
    }

    public static OutboxEvent newEvent(
            String tenantId,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            String payload,
            Instant occurredAt
    ) {
        return new OutboxEvent(
                UUID.randomUUID(),
                tenantId,
                aggregateType,
                aggregateId,
                eventType,
                payload,
                OutboxEventStatus.NEW,
                occurredAt,
                null,
                0,
                null
        );
    }
}
