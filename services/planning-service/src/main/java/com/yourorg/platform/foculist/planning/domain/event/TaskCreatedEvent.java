package com.yourorg.platform.foculist.planning.domain.event;

import java.time.Instant;
import java.util.UUID;

public record TaskCreatedEvent(
        UUID taskId,
        String tenantId,
        UUID sprintId,
        String title,
        String description,
        String status,
        long version,
        Instant occurredAt
) implements TaskEvent {}
