package com.yourorg.platform.foculist.project.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.project.domain.port.ProjectMemberRepositoryPort;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaProjectMemberRepositoryAdapter implements ProjectMemberRepositoryPort {
    private final ProjectMemberJpaRepository repository;

    @Override
    public Optional<String> getRole(UUID projectId, UUID userId, String tenantId) {
        return repository.findByProjectIdAndUserIdAndTenantId(projectId, userId, tenantId)
                .map(ProjectMemberJpaEntity::getRole);
    }
}
