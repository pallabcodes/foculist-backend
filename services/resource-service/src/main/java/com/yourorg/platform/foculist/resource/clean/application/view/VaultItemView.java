package com.yourorg.platform.foculist.resource.clean.application.view;

import java.time.Instant;
import java.util.UUID;

public record VaultItemView(
        UUID id,
        String name,
        String classification,
        String tenantId,
        Instant createdAt
) {
}
