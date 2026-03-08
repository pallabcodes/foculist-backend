package com.yourorg.platform.foculist.sync.clean.domain.port;

import com.yourorg.platform.foculist.sync.clean.domain.model.SyncDeviceCursor;
import java.util.Optional;

public interface SyncDeviceCursorRepositoryPort {
    Optional<SyncDeviceCursor> findByTenantIdAndDeviceId(String tenantId, String deviceId);

    SyncDeviceCursor save(SyncDeviceCursor syncDeviceCursor);
}
