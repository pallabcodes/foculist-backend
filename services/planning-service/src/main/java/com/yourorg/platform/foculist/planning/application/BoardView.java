package com.yourorg.platform.foculist.planning.application;

import java.time.Instant;
import java.util.UUID;

public record BoardView(
        UUID id,
        UUID projectId,
        String name,
        String type,
        Instant createdAt,
        String tenantId,
        Long version
) {}
