package com.yourorg.platform.foculist.planning.domain.model;

import java.time.Instant;
import java.util.UUID;

public record ProjectionCheckpoint(
        String projectionName,
        Instant lastOccurredAt,
        UUID lastEventId,
        Instant updatedAt
) {
    public ProjectionCheckpoint {
        if (projectionName == null || projectionName.isBlank()) {
            throw new PlanningDomainException("Projection name is required");
        }
    }

    public static ProjectionCheckpoint initial(String projectionName) {
        return new ProjectionCheckpoint(projectionName, null, null, null);
    }

    public ProjectionCheckpoint advance(Instant occurredAt, UUID eventId, Instant now) {
        return new ProjectionCheckpoint(projectionName, occurredAt, eventId, now);
    }
}
