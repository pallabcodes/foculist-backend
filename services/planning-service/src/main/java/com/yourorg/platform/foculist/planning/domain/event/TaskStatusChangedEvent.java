package com.yourorg.platform.foculist.planning.domain.event;

import java.time.Instant;
import java.util.UUID;

public record TaskStatusChangedEvent(
        UUID taskId,
        String tenantId,
        String oldStatus,
        String newStatus,
        long version,
        Instant occurredAt
) implements TaskEvent {}
