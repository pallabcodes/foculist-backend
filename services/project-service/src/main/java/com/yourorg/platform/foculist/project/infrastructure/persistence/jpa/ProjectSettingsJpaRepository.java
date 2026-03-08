package com.yourorg.platform.foculist.project.infrastructure.persistence.jpa;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectSettingsJpaRepository extends JpaRepository<ProjectSettingsJpaEntity, UUID> {
    Optional<ProjectSettingsJpaEntity> findByProjectIdAndTenantId(UUID projectId, String tenantId);
}
