package com.yourorg.platform.foculist.planning.infrastructure.messaging;

import com.yourorg.platform.foculist.planning.application.TaskReplayEngine;
import com.yourorg.platform.foculist.planning.domain.model.Task;
import com.yourorg.platform.foculist.planning.domain.model.TaskSnapshot;
import com.yourorg.platform.foculist.planning.domain.model.TaskSnapshotJob;
import com.yourorg.platform.foculist.planning.domain.port.TaskSnapshotJobRepositoryPort;
import com.yourorg.platform.foculist.planning.infrastructure.persistence.mongodb.TaskSnapshotDocument;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PlanningTaskSnapshotWorker {
    private static final Logger log = LoggerFactory.getLogger(PlanningTaskSnapshotWorker.class);

    private final TaskSnapshotJobRepositoryPort snapshotJobRepository;
    private final MongoTemplate mongoTemplate;
    private final TaskReplayEngine replayEngine;
    private final Clock clock;

    @Value("${app.event-sourcing.snapshot-batch-size:25}")
    private int batchSize;

    @Value("${app.event-sourcing.snapshot-max-attempts:10}")
    private int maxAttempts;

    public PlanningTaskSnapshotWorker(
            TaskSnapshotJobRepositoryPort snapshotJobRepository,
            MongoTemplate mongoTemplate,
            TaskReplayEngine replayEngine,
            org.springframework.beans.factory.ObjectProvider<Clock> clockProvider
    ) {
        this.snapshotJobRepository = snapshotJobRepository;
        this.mongoTemplate = mongoTemplate;
        this.replayEngine = replayEngine;
        this.clock = clockProvider.getIfAvailable(Clock::systemUTC);
    }

    @Scheduled(fixedDelayString = "${app.event-sourcing.snapshot-poll-interval-ms:5000}")
    public void snapshot() {
        List<TaskSnapshotJob> jobs = snapshotJobRepository.claimPendingBatch(batchSize, maxAttempts);
        if (jobs.isEmpty()) {
            return;
        }

        BulkOperations ops = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, TaskSnapshotDocument.class);
        boolean hasOps = false;

        for (TaskSnapshotJob job : jobs) {
            try {
                Task task = replayEngine.restoreAtVersion(job.tenantId(), job.taskId(), job.targetVersion());
                TaskSnapshot snapshot = TaskSnapshot.fromTask(task, Instant.now(clock));
                
                Query query = Query.query(Criteria.where("taskId").is(snapshot.taskId()).and("version").is(snapshot.version()));
                Update update = new Update()
                        .set("tenantId", snapshot.tenantId())
                        .set("taskId", snapshot.taskId())
                        .set("sprintId", snapshot.sprintId())
                        .set("title", snapshot.title())
                        .set("description", snapshot.description())
                        .set("status", snapshot.status())
                        .set("priority", snapshot.priority())
                        .set("createdAt", snapshot.createdAt())
                        .set("updatedAt", snapshot.updatedAt())
                        .set("createdBy", snapshot.createdBy())
                        .set("updatedBy", snapshot.updatedBy())
                        .set("deletedAt", snapshot.deletedAt())
                        .set("metadata", snapshot.metadata())
                        .set("version", snapshot.version())
                        .set("snapshottedAt", snapshot.snapshottedAt());
                
                ops.upsert(query, update);
                hasOps = true;
                
                snapshotJobRepository.markCompleted(job.id(), Instant.now(clock));
            } catch (Exception ex) {
                snapshotJobRepository.markFailed(job.id(), truncateError(ex.getMessage()));
                log.warn("Failed to build task snapshot taskId={} version={} error={}",
                        job.taskId(), job.targetVersion(), ex.getMessage());
            }
        }

        if (hasOps) {
            ops.execute();
            log.info("Batch processed {} task snapshots into MongoDB", jobs.size());
        }
    }

    private String truncateError(String message) {
        if (message == null || message.length() <= 500) {
            return message;
        }
        return message.substring(0, 500);
    }
}

