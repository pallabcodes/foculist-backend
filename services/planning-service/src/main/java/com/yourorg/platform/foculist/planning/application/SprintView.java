package com.yourorg.platform.foculist.planning.application;

import java.time.Instant;
import java.util.UUID;

public record SprintView(
        UUID id,
        String name,
        String status,
        Instant startDate,
        Instant endDate,
        String tenantId,
        Long version
) {
}
