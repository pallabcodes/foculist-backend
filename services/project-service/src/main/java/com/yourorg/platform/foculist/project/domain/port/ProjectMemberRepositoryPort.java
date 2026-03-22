package com.yourorg.platform.foculist.project.domain.port;

import java.util.Optional;
import java.util.UUID;

public interface ProjectMemberRepositoryPort {
    Optional<String> getRole(UUID projectId, UUID userId, String tenantId);
}
