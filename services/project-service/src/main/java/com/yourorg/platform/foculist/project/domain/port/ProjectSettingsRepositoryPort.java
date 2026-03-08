package com.yourorg.platform.foculist.project.domain.port;

import com.yourorg.platform.foculist.project.domain.model.ProjectSettings;
import java.util.Optional;
import java.util.UUID;

public interface ProjectSettingsRepositoryPort {
    Optional<ProjectSettings> findByProjectIdAndTenantId(UUID projectId, String tenantId);

    ProjectSettings save(ProjectSettings projectSettings);
}
