package com.yourorg.platform.foculist.planning.domain.port;

import com.yourorg.platform.foculist.planning.domain.model.TaskSnapshot;
import java.util.Optional;
import java.util.UUID;

public interface TaskSnapshotRepositoryPort {
    TaskSnapshot save(TaskSnapshot snapshot);
    Optional<TaskSnapshot> findLatestByTaskIdAndTenantId(UUID taskId, String tenantId);
    Optional<TaskSnapshot> findLatestByTaskIdAndTenantIdUpToVersion(UUID taskId, String tenantId, long version);
}
