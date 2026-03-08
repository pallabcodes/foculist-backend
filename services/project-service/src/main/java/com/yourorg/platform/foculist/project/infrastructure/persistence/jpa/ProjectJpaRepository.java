package com.yourorg.platform.foculist.project.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectJpaRepository extends JpaRepository<ProjectJpaEntity, UUID> {
    List<ProjectJpaEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    Page<ProjectJpaEntity> findByTenantId(String tenantId, Pageable pageable);

    Optional<ProjectJpaEntity> findByIdAndTenantId(UUID id, String tenantId);
}
