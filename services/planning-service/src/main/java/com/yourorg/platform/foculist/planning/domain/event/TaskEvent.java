package com.yourorg.platform.foculist.planning.domain.event;

import java.time.Instant;
import java.util.UUID;

public sealed interface TaskEvent permits TaskCreatedEvent, TaskUpdatedEvent, TaskStatusChangedEvent, TaskDeletedEvent {
    UUID taskId();
    String tenantId();
    Instant occurredAt();
    long version();
}
