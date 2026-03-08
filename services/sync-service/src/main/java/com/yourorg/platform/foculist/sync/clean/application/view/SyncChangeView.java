package com.yourorg.platform.foculist.sync.clean.application.view;

import java.time.Instant;
import java.util.UUID;

public record SyncChangeView(
        UUID id,
        String type,
        String deviceId,
        String payload,
        Instant eventTime
) {
}
