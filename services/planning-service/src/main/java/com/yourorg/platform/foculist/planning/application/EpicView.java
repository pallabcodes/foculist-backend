package com.yourorg.platform.foculist.planning.application;

import java.time.Instant;
import java.util.UUID;

public record EpicView(
        UUID id,
        UUID projectId,
        String name,
        String summary,
        String color,
        String status,
        Instant startDate,
        Instant targetDate,
        Instant createdAt,
        String tenantId,
        Long version
) {}
