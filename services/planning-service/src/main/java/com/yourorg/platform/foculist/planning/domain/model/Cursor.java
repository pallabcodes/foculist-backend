package com.yourorg.platform.foculist.planning.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Cursor(
        Instant createdAt,
        UUID id
) {}
