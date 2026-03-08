package com.yourorg.platform.foculist.planning.domain.port;

import com.yourorg.platform.foculist.planning.domain.model.TaskSnapshotJob;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface TaskSnapshotJobRepositoryPort {
    void saveIfAbsent(TaskSnapshotJob job);
    List<TaskSnapshotJob> claimPendingBatch(int batchSize, int maxAttempts);
    void markCompleted(UUID jobId, Instant processedAt);
    void markFailed(UUID jobId, String errorMessage);
}
