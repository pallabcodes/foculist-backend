package com.yourorg.platform.foculist.planning.domain.event;

import java.time.Instant;
import java.util.UUID;

public record TaskUpdatedEvent(
        UUID taskId,
        String tenantId,
        UUID sprintId,
        String title,
        String description,
        String priority,
        long version,
        Instant occurredAt
) implements TaskEvent {}
