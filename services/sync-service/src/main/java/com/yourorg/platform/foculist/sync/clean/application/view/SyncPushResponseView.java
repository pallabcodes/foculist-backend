package com.yourorg.platform.foculist.sync.clean.application.view;

import java.time.Instant;
import java.util.UUID;

public record SyncPushResponseView(
        boolean accepted,
        UUID envelopeId,
        String deviceId,
        int pendingChanges,
        Instant receivedAt,
        String tenantId
) {
}
