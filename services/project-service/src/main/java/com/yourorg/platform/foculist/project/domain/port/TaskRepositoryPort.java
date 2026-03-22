package com.yourorg.platform.foculist.project.domain.port;

import com.yourorg.platform.foculist.project.domain.model.Task;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepositoryPort {
    Task save(Task task);
    Optional<Task> findByIdAndTenantId(UUID id, String tenantId);
    List<Task> findByProjectIdAndTenantId(UUID projectId, String tenantId, int page, int size);
    void delete(UUID id, String tenantId);
}
