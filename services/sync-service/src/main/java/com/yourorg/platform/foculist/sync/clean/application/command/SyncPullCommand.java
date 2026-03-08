package com.yourorg.platform.foculist.sync.clean.application.command;

public record SyncPullCommand(
        String deviceId,
        String lastSync
) {
}
