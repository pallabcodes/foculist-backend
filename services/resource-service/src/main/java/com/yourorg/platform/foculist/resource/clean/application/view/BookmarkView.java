package com.yourorg.platform.foculist.resource.clean.application.view;

import java.time.Instant;
import java.util.UUID;

public record BookmarkView(
        UUID id,
        String title,
        String url,
        String tenantId,
        Instant createdAt
) {
}
