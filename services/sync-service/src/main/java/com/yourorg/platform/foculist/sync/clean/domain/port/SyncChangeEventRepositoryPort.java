package com.yourorg.platform.foculist.sync.clean.domain.port;

import com.yourorg.platform.foculist.sync.clean.domain.model.SyncChangeEvent;
import java.time.Instant;
import java.util.List;

public interface SyncChangeEventRepositoryPort {
    SyncChangeEvent save(SyncChangeEvent syncChangeEvent);

    List<SyncChangeEvent> findSince(String tenantId, Instant sinceExclusive, int limit);
}
