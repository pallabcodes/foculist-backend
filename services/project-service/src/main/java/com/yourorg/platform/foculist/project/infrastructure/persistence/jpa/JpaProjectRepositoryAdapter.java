package com.yourorg.platform.foculist.project.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.project.domain.model.Project;
import com.yourorg.platform.foculist.project.domain.model.ProjectPriority;
import com.yourorg.platform.foculist.project.domain.model.ProjectStatus;
import com.yourorg.platform.foculist.project.domain.port.ProjectRepositoryPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JpaProjectRepositoryAdapter implements ProjectRepositoryPort {
    private final ProjectJpaRepository projectJpaRepository;

    public JpaProjectRepositoryAdapter(ProjectJpaRepository projectJpaRepository) {
        this.projectJpaRepository = projectJpaRepository;
    }

    @Override
    public List<Project> findByTenantId(String tenantId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return projectJpaRepository.findByTenantId(tenantId, pageable).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Project> findByIdAndTenantId(UUID projectId, String tenantId) {
        return projectJpaRepository.findByIdAndTenantId(projectId, tenantId)
                .map(this::toDomain);
    }

    @Override
    public Project save(Project project) {
        return toDomain(projectJpaRepository.save(toEntity(project)));
    }

    private ProjectJpaEntity toEntity(Project domain) {
        ProjectJpaEntity entity = new ProjectJpaEntity();
        entity.setId(domain.id());
        entity.setTenantId(domain.tenantId());
        entity.setName(domain.name());
        entity.setDescription(domain.description());
        entity.setStatus(domain.status().name());
        entity.setPriority(domain.priority().name());
        entity.setDueDate(domain.dueDate());
        entity.setOwnerId(domain.ownerId());
        entity.setKey(domain.key());
        entity.setPermissionSchemeId(domain.permissionSchemeId());
        entity.setCreatedAt(domain.createdAt());
        entity.setUpdatedAt(domain.updatedAt());
        entity.setCreatedBy(domain.createdBy());
        entity.setUpdatedBy(domain.updatedBy());
        entity.setDeletedAt(domain.deletedAt());
        entity.setMetadata(domain.metadata());
        entity.setVersion(domain.version());
        return entity;
    }

    private Project toDomain(ProjectJpaEntity entity) {
        return new Project(
                entity.getId(),
                entity.getTenantId(),
                entity.getName(),
                entity.getDescription(),
                ProjectStatus.valueOf(entity.getStatus()),
                ProjectPriority.valueOf(entity.getPriority()),
                entity.getDueDate(),
                entity.getOwnerId(),
                entity.getKey(),
                entity.getPermissionSchemeId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedBy(),
                entity.getDeletedAt(),
                entity.getMetadata(),
                entity.getVersion()
        );
    }
}
