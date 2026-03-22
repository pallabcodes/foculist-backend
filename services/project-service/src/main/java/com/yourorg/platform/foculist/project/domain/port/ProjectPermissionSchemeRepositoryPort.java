package com.yourorg.platform.foculist.project.domain.port;

import com.yourorg.platform.foculist.project.domain.model.ProjectPermissionScheme;
import java.util.Optional;
import java.util.UUID;

public interface ProjectPermissionSchemeRepositoryPort {
    Optional<ProjectPermissionScheme> findByIdAndTenantId(UUID id, String tenantId);
    Optional<ProjectPermissionScheme> findByTenantIdAndName(String tenantId, String name);
    ProjectPermissionScheme save(ProjectPermissionScheme scheme);
}
