package com.yourorg.platform.foculist.project.domain.port;

import com.yourorg.platform.foculist.project.domain.model.Project;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepositoryPort {
    List<Project> findByTenantId(String tenantId, int page, int size);

    Optional<Project> findByIdAndTenantId(UUID projectId, String tenantId);

    Project save(Project project);
}
