package com.yourorg.platform.foculist.sync.clean.infrastructure.oplog;

import com.yourorg.platform.foculist.sync.clean.domain.model.SyncRealtimeOpLogEntry;
import com.yourorg.platform.foculist.sync.clean.domain.port.SyncRealtimeOpLogRepositoryPort;

public class NoopSyncRealtimeOpLogRepositoryAdapter implements SyncRealtimeOpLogRepositoryPort {
    @Override
    public void append(SyncRealtimeOpLogEntry entry) {
        // No-op when DynamoDB op log mode is disabled.
    }
}
