package com.yourorg.platform.foculist.sync.clean.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncChangeEventJpaRepository extends JpaRepository<SyncChangeEventJpaEntity, UUID> {
    List<SyncChangeEventJpaEntity> findByTenantIdAndOccurredAtGreaterThanOrderByOccurredAtAsc(
            String tenantId,
            Instant occurredAt,
            Pageable pageable
    );
}
