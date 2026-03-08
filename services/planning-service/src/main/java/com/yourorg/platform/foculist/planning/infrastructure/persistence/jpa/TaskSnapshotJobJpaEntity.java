package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.planning.domain.model.OutboxEventStatus;
import com.yourorg.platform.foculist.planning.domain.model.TaskSnapshotJob;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "planning_task_snapshot_job",
        indexes = {
                @Index(name = "idx_planning_snapshot_job_status_created", columnList = "status,created_at"),
                @Index(name = "idx_planning_snapshot_job_task", columnList = "tenant_id,task_id,target_version")
        }
)
public class TaskSnapshotJobJpaEntity {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 128)
    private String tenantId;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "target_version", nullable = false)
    private long targetVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OutboxEventStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected TaskSnapshotJobJpaEntity() {
    }

    TaskSnapshotJobJpaEntity(
            UUID id,
            String tenantId,
            UUID taskId,
            long targetVersion,
            OutboxEventStatus status,
            int attempts,
            String lastError,
            Instant createdAt,
            Instant processedAt,
            long version
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.taskId = taskId;
        this.targetVersion = targetVersion;
        this.status = status;
        this.attempts = attempts;
        this.lastError = lastError;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
        this.version = version;
    }

    public static TaskSnapshotJobJpaEntity fromDomain(TaskSnapshotJob job) {
        return new TaskSnapshotJobJpaEntity(
                job.id(),
                job.tenantId(),
                job.taskId(),
                job.targetVersion(),
                job.status(),
                job.attempts(),
                job.lastError(),
                job.createdAt(),
                job.processedAt(),
                0L
        );
    }

    public TaskSnapshotJob toDomain() {
        return new TaskSnapshotJob(
                id,
                tenantId,
                taskId,
                targetVersion,
                status,
                attempts,
                lastError,
                createdAt,
                processedAt
        );
    }

    public void markProcessing() {
        this.status = OutboxEventStatus.PROCESSING;
    }

    public void markCompleted(Instant processedAt) {
        this.status = OutboxEventStatus.PUBLISHED;
        this.processedAt = processedAt;
        this.lastError = null;
    }

    public void markFailed(String errorMessage) {
        this.status = OutboxEventStatus.FAILED;
        this.lastError = errorMessage;
        this.attempts = this.attempts + 1;
    }
}
