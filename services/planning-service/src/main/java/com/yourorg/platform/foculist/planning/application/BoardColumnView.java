package com.yourorg.platform.foculist.planning.application;

import java.time.Instant;
import java.util.UUID;

public record BoardColumnView(
        UUID id,
        UUID boardId,
        String name,
        String statusMapping,
        int orderIndex,
        Instant createdAt,
        String tenantId,
        Long version
) {}
