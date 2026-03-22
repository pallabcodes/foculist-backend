package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardJpaRepository extends JpaRepository<BoardJpaEntity, UUID> {
    Optional<BoardJpaEntity> findByIdAndTenantId(UUID id, String tenantId);
    Page<BoardJpaEntity> findByTenantId(String tenantId, Pageable pageable);
    Page<BoardJpaEntity> findByProjectIdAndTenantId(UUID projectId, String tenantId, Pageable pageable);
}
