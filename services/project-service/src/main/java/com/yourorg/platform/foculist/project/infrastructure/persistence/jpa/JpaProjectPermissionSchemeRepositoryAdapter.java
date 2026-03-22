package com.yourorg.platform.foculist.project.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.project.domain.model.ProjectPermissionScheme;
import com.yourorg.platform.foculist.project.domain.port.ProjectPermissionSchemeRepositoryPort;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaProjectPermissionSchemeRepositoryAdapter implements ProjectPermissionSchemeRepositoryPort {
    private final ProjectPermissionSchemeJpaRepository repository;

    @Override
    public Optional<ProjectPermissionScheme> findByIdAndTenantId(UUID id, String tenantId) {
        return repository.findByIdAndTenantId(id, tenantId).map(this::toDomain);
    }

    @Override
    public Optional<ProjectPermissionScheme> findByTenantIdAndName(String tenantId, String name) {
        return repository.findByTenantIdAndName(tenantId, name).map(this::toDomain);
    }

    @Override
    public ProjectPermissionScheme save(ProjectPermissionScheme scheme) {
        ProjectPermissionSchemeJpaEntity entity = toEntity(scheme);
        ProjectPermissionSchemeJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    private ProjectPermissionScheme toDomain(ProjectPermissionSchemeJpaEntity entity) {
        return ProjectPermissionScheme.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .name(entity.getName())
                .description(entity.getDescription())
                .actionsMapping(entity.getActionsMapping())
                .build();
    }

    private ProjectPermissionSchemeJpaEntity toEntity(ProjectPermissionScheme domain) {
        ProjectPermissionSchemeJpaEntity entity = new ProjectPermissionSchemeJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setActionsMapping(domain.getActionsMapping());
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }
}
