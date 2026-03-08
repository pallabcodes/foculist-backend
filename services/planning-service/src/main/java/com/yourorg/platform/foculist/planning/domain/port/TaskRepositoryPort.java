package com.yourorg.platform.foculist.planning.domain.port;

import com.yourorg.platform.foculist.planning.domain.model.Task;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

public interface TaskRepositoryPort {
    List<Task> findByTenantId(String tenantId, int page, int size, Instant cursorCreatedAt, UUID cursorId);
    Optional<Task> findByIdAndTenantId(UUID id, String tenantId);
    Task save(Task task);
    void delete(Task task);
}
