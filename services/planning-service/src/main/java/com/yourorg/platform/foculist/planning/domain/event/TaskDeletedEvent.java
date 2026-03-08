package com.yourorg.platform.foculist.planning.domain.event;

import java.time.Instant;
import java.util.UUID;

public record TaskDeletedEvent(
        UUID taskId,
        String tenantId,
        long version,
        Instant occurredAt
) implements TaskEvent {}
