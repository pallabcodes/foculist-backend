package com.yourorg.platform.foculist.planning.domain.event;

import java.time.Instant;
import java.util.UUID;

public record TaskUpdatedEvent(
        UUID taskId,
        String tenantId,
        String title,
        String description,
        long version,
        Instant occurredAt
) implements TaskEvent {}
