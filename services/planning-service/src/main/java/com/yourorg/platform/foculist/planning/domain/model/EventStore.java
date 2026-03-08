package com.yourorg.platform.foculist.planning.domain.model;

import java.time.Instant;
import java.util.UUID;

public record EventStore(
        UUID id,
        String tenantId,
        UUID aggregateId,
        String aggregateType,
        String eventType,
        String payload,
        long version,
        Instant occurredAt
) {
    public EventStore {
        if (id == null) throw new PlanningDomainException("Event id is required");
        if (tenantId == null || tenantId.isBlank()) throw new PlanningDomainException("Tenant id is required");
        if (aggregateId == null) throw new PlanningDomainException("Aggregate id is required");
        if (aggregateType == null || aggregateType.isBlank()) throw new PlanningDomainException("Aggregate type is required");
        if (eventType == null || eventType.isBlank()) throw new PlanningDomainException("Event type is required");
        if (payload == null) throw new PlanningDomainException("Payload is required");
        if (occurredAt == null) throw new PlanningDomainException("OccurredAt is required");
    }
}
