package com.yourorg.platform.foculist.sync.clean.infrastructure.persistence.jpa;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncDeviceCursorJpaRepository extends JpaRepository<SyncDeviceCursorJpaEntity, UUID> {
    Optional<SyncDeviceCursorJpaEntity> findByTenantIdAndDeviceId(String tenantId, String deviceId);
}
