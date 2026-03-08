package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskSnapshotJpaRepository extends JpaRepository<TaskSnapshotJpaEntity, UUID> {
    Optional<TaskSnapshotJpaEntity> findTopByTaskIdAndTenantIdOrderByVersionDesc(UUID taskId, String tenantId);
    Optional<TaskSnapshotJpaEntity> findTopByTaskIdAndTenantIdAndVersionLessThanEqualOrderByVersionDesc(
            UUID taskId,
            String tenantId,
            long version
    );
}
