package com.yourorg.platform.foculist.planning.infrastructure.messaging;

import com.yourorg.platform.foculist.planning.application.TaskReplayEngine;
import com.yourorg.platform.foculist.planning.domain.model.Task;
import com.yourorg.platform.foculist.planning.domain.model.TaskSnapshot;
import com.yourorg.platform.foculist.planning.domain.model.TaskSnapshotJob;
import com.yourorg.platform.foculist.planning.domain.port.TaskSnapshotJobRepositoryPort;
import com.yourorg.platform.foculist.planning.domain.port.TaskSnapshotRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PlanningTaskSnapshotWorker {
    private static final Logger log = LoggerFactory.getLogger(PlanningTaskSnapshotWorker.class);

    private final TaskSnapshotJobRepositoryPort snapshotJobRepository;
    private final TaskSnapshotRepositoryPort taskSnapshotRepository;
    private final TaskReplayEngine replayEngine;
    private final Clock clock;

    @Value("${app.event-sourcing.snapshot-batch-size:25}")
    private int batchSize;

    @Value("${app.event-sourcing.snapshot-max-attempts:10}")
    private int maxAttempts;

    public PlanningTaskSnapshotWorker(
            TaskSnapshotJobRepositoryPort snapshotJobRepository,
            TaskSnapshotRepositoryPort taskSnapshotRepository,
            TaskReplayEngine replayEngine
    ) {
        this(snapshotJobRepository, taskSnapshotRepository, replayEngine, Clock.systemUTC());
    }

    PlanningTaskSnapshotWorker(
            TaskSnapshotJobRepositoryPort snapshotJobRepository,
            TaskSnapshotRepositoryPort taskSnapshotRepository,
            TaskReplayEngine replayEngine,
            Clock clock
    ) {
        this.snapshotJobRepository = snapshotJobRepository;
        this.taskSnapshotRepository = taskSnapshotRepository;
        this.replayEngine = replayEngine;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.event-sourcing.snapshot-poll-interval-ms:5000}")
    public void snapshot() {
        List<TaskSnapshotJob> jobs = snapshotJobRepository.claimPendingBatch(batchSize, maxAttempts);
        for (TaskSnapshotJob job : jobs) {
            try {
                Task task = replayEngine.restoreAtVersion(job.tenantId(), job.taskId(), job.targetVersion());
                taskSnapshotRepository.save(TaskSnapshot.fromTask(task, Instant.now(clock)));
                snapshotJobRepository.markCompleted(job.id(), Instant.now(clock));
                log.info("Created task snapshot taskId={} version={}", job.taskId(), job.targetVersion());
            } catch (Exception ex) {
                snapshotJobRepository.markFailed(job.id(), truncateError(ex.getMessage()));
                log.warn("Failed to build task snapshot taskId={} version={} error={}",
                        job.taskId(), job.targetVersion(), ex.getMessage());
            }
        }
    }

    private String truncateError(String message) {
        if (message == null || message.length() <= 500) {
            return message;
        }
        return message.substring(0, 500);
    }
}
