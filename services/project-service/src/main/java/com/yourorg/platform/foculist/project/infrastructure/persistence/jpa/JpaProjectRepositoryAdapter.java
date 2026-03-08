package com.yourorg.platform.foculist.project.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.project.domain.model.Project;
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
                .map(ProjectJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Project> findByIdAndTenantId(UUID projectId, String tenantId) {
        return projectJpaRepository.findByIdAndTenantId(projectId, tenantId)
                .map(ProjectJpaEntity::toDomain);
    }

    @Override
    public Project save(Project project) {
        return projectJpaRepository.save(ProjectJpaEntity.fromDomain(project)).toDomain();
    }
}
