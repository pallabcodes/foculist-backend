package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.planning.domain.model.Task;
import com.yourorg.platform.foculist.planning.domain.port.TaskRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

@Component
public class JpaTaskRepositoryAdapter implements TaskRepositoryPort {
    private final TaskJpaRepository taskJpaRepository;

    public JpaTaskRepositoryAdapter(TaskJpaRepository taskJpaRepository) {
        this.taskJpaRepository = taskJpaRepository;
    }

    @Override
    public List<Task> findByTenantId(String tenantId, int page, int size, Instant cursorCreatedAt, UUID cursorId) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        var stream = (cursorCreatedAt != null && cursorId != null)
                ? taskJpaRepository.findByTenantIdBeforeCursor(tenantId, cursorCreatedAt, cursorId, pageable).stream()
                : taskJpaRepository.findByTenantId(tenantId, pageable).stream();
        return stream.map(TaskJpaEntity::toDomain).toList();
    }

    @Override
    public Optional<Task> findByIdAndTenantId(UUID id, String tenantId) {
        return taskJpaRepository.findByIdAndTenantId(id, tenantId)
                .map(TaskJpaEntity::toDomain);
    }

    @Override
    public Task save(Task task) {
        return taskJpaRepository.save(TaskJpaEntity.fromDomain(task)).toDomain();
    }

    @Override
    public void delete(Task task) {
        taskJpaRepository.delete(TaskJpaEntity.fromDomain(task));
    }
}
