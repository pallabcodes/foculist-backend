package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EpicJpaRepository extends JpaRepository<EpicJpaEntity, UUID> {
    Optional<EpicJpaEntity> findByIdAndTenantId(UUID id, String tenantId);
    Page<EpicJpaEntity> findByTenantId(String tenantId, Pageable pageable);
    Page<EpicJpaEntity> findByProjectIdAndTenantId(UUID projectId, String tenantId, Pageable pageable);
}
