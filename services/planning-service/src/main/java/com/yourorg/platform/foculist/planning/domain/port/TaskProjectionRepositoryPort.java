package com.yourorg.platform.foculist.planning.domain.port;

import com.yourorg.platform.foculist.planning.domain.model.TaskProjection;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskProjectionRepositoryPort {
    List<TaskProjection> findByTenantId(String tenantId, int page, int size, Instant cursorCreatedAt, UUID cursorId);
    Optional<TaskProjection> findByIdAndTenantId(UUID id, String tenantId);
    TaskProjection save(TaskProjection projection);
    void deleteByIdAndTenantId(UUID id, String tenantId);
}
