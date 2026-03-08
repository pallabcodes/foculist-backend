package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.planning.domain.model.TaskProjection;
import com.yourorg.platform.foculist.planning.domain.port.TaskProjectionRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class JpaTaskProjectionRepositoryAdapter implements TaskProjectionRepositoryPort {
    private final TaskProjectionJpaRepository repository;

    public JpaTaskProjectionRepositoryAdapter(TaskProjectionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<TaskProjection> findByTenantId(String tenantId, int page, int size, Instant cursorCreatedAt, UUID cursorId) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        return (cursorCreatedAt != null && cursorId != null
                ? repository.findByTenantIdBeforeCursor(tenantId, cursorCreatedAt, cursorId, pageable)
                : repository.findByTenantId(tenantId, pageable)).stream()
                .map(TaskProjectionJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<TaskProjection> findByIdAndTenantId(UUID id, String tenantId) {
        return repository.findByIdAndTenantId(id, tenantId).map(TaskProjectionJpaEntity::toDomain);
    }

    @Override
    public TaskProjection save(TaskProjection projection) {
        return repository.save(TaskProjectionJpaEntity.fromDomain(projection)).toDomain();
    }

    @Override
    public void deleteByIdAndTenantId(UUID id, String tenantId) {
        repository.deleteByIdAndTenantId(id, tenantId);
    }
}
