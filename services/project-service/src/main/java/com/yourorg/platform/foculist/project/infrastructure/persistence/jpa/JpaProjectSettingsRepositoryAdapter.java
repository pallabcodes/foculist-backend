package com.yourorg.platform.foculist.project.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.project.domain.model.ProjectSettings;
import com.yourorg.platform.foculist.project.domain.port.ProjectSettingsRepositoryPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JpaProjectSettingsRepositoryAdapter implements ProjectSettingsRepositoryPort {
    private final ProjectSettingsJpaRepository projectSettingsJpaRepository;

    public JpaProjectSettingsRepositoryAdapter(ProjectSettingsJpaRepository projectSettingsJpaRepository) {
        this.projectSettingsJpaRepository = projectSettingsJpaRepository;
    }

    @Override
    public Optional<ProjectSettings> findByProjectIdAndTenantId(UUID projectId, String tenantId) {
        return projectSettingsJpaRepository.findByProjectIdAndTenantId(projectId, tenantId)
                .map(ProjectSettingsJpaEntity::toDomain);
    }

    @Override
    public ProjectSettings save(ProjectSettings projectSettings) {
        return projectSettingsJpaRepository.save(ProjectSettingsJpaEntity.fromDomain(projectSettings)).toDomain();
    }
}
