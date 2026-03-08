package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.planning.domain.model.TaskSnapshot;
import com.yourorg.platform.foculist.planning.domain.port.TaskSnapshotRepositoryPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JpaTaskSnapshotRepositoryAdapter implements TaskSnapshotRepositoryPort {
    private final TaskSnapshotJpaRepository repository;

    public JpaTaskSnapshotRepositoryAdapter(TaskSnapshotJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public TaskSnapshot save(TaskSnapshot snapshot) {
        return repository.save(TaskSnapshotJpaEntity.fromDomain(snapshot)).toDomain();
    }

    @Override
    public Optional<TaskSnapshot> findLatestByTaskIdAndTenantId(UUID taskId, String tenantId) {
        return repository.findTopByTaskIdAndTenantIdOrderByVersionDesc(taskId, tenantId)
                .map(TaskSnapshotJpaEntity::toDomain);
    }

    @Override
    public Optional<TaskSnapshot> findLatestByTaskIdAndTenantIdUpToVersion(UUID taskId, String tenantId, long version) {
        return repository.findTopByTaskIdAndTenantIdAndVersionLessThanEqualOrderByVersionDesc(taskId, tenantId, version)
                .map(TaskSnapshotJpaEntity::toDomain);
    }
}
