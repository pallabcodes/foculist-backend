package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SprintJpaRepository extends JpaRepository<SprintJpaEntity, UUID> {
    List<SprintJpaEntity> findByTenantIdOrderByStartDateDesc(String tenantId);

    Page<SprintJpaEntity> findByTenantId(String tenantId, Pageable pageable);

    Optional<SprintJpaEntity> findByIdAndTenantId(UUID id, String tenantId);
}
