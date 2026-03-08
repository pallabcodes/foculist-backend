package com.yourorg.platform.foculist.sync.clean.application.command;

import java.util.Map;

public record SyncPushCommand(
        String deviceId,
        int pendingChanges,
        String payloadVersion,
        Map<String, Object> payload,
        String clientSyncTime
) {
}
