package com.yourorg.platform.foculist.resource.clean.application.view;

import java.time.Instant;
import java.util.UUID;

public record WorklogEntryView(
        UUID id,
        String project,
        String task,
        int durationMinutes,
        String tenantId,
        Instant loggedAt
) {
}
