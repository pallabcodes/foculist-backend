package com.yourorg.platform.foculist.sync.clean.domain.port;

import com.yourorg.platform.foculist.sync.clean.domain.model.SyncRealtimeOpLogEntry;

public interface SyncRealtimeOpLogRepositoryPort {
    void append(SyncRealtimeOpLogEntry entry);
}
