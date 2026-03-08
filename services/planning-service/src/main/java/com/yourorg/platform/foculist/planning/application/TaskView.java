package com.yourorg.platform.foculist.planning.application;

import java.time.Instant;
import java.util.UUID;

public record TaskView(
        UUID id,
        UUID sprintId,
        String title,
        String description,
        String status,
        String priority,
        Instant createdAt,
        String tenantId,
        Long version
) {
}
