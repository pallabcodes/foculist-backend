package com.yourorg.platform.foculist.sync.clean.application.view;

import java.time.Instant;
import java.util.List;

public record SyncPullResponseView(
        String lastSync,
        List<SyncChangeView> changes,
        int changeCount,
        Instant serverTime,
        Instant nextSyncCursor,
        String tenantId
) {
}
