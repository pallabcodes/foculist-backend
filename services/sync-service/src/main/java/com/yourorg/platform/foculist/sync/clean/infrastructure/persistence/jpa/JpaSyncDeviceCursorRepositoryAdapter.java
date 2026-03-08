package com.yourorg.platform.foculist.sync.clean.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.sync.clean.domain.model.SyncDeviceCursor;
import com.yourorg.platform.foculist.sync.clean.domain.port.SyncDeviceCursorRepositoryPort;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaSyncDeviceCursorRepositoryAdapter implements SyncDeviceCursorRepositoryPort {
    private final SyncDeviceCursorJpaRepository repository;

    public JpaSyncDeviceCursorRepositoryAdapter(SyncDeviceCursorJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<SyncDeviceCursor> findByTenantIdAndDeviceId(String tenantId, String deviceId) {
        return repository.findByTenantIdAndDeviceId(tenantId, deviceId).map(SyncDeviceCursorJpaEntity::toDomain);
    }

    @Override
    public SyncDeviceCursor save(SyncDeviceCursor syncDeviceCursor) {
        return repository.save(SyncDeviceCursorJpaEntity.fromDomain(syncDeviceCursor)).toDomain();
    }
}
