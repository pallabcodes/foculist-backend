package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.planning.domain.model.OutboxEventStatus;
import com.yourorg.platform.foculist.planning.domain.model.PlanningDomainException;
import com.yourorg.platform.foculist.planning.domain.model.TaskSnapshotJob;
import com.yourorg.platform.foculist.planning.domain.port.TaskSnapshotJobRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaTaskSnapshotJobRepositoryAdapter implements TaskSnapshotJobRepositoryPort {
    private final TaskSnapshotJobJpaRepository repository;

    public JpaTaskSnapshotJobRepositoryAdapter(TaskSnapshotJobJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void saveIfAbsent(TaskSnapshotJob job) {
        if (!repository.existsByTenantIdAndTaskIdAndTargetVersion(job.tenantId(), job.taskId(), job.targetVersion())) {
            repository.save(TaskSnapshotJobJpaEntity.fromDomain(job));
        }
    }

    @Override
    @Transactional
    public List<TaskSnapshotJob> claimPendingBatch(int batchSize, int maxAttempts) {
        List<TaskSnapshotJobJpaEntity> entities = repository.findForProcessing(
                List.of(OutboxEventStatus.NEW, OutboxEventStatus.FAILED),
                maxAttempts,
                PageRequest.of(0, batchSize)
        );
        entities.forEach(TaskSnapshotJobJpaEntity::markProcessing);
        return entities.stream().map(TaskSnapshotJobJpaEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public void markCompleted(UUID jobId, Instant processedAt) {
        TaskSnapshotJobJpaEntity entity = repository.findById(jobId)
                .orElseThrow(() -> new PlanningDomainException("Snapshot job does not exist: " + jobId));
        entity.markCompleted(processedAt);
    }

    @Override
    @Transactional
    public void markFailed(UUID jobId, String errorMessage) {
        TaskSnapshotJobJpaEntity entity = repository.findById(jobId)
                .orElseThrow(() -> new PlanningDomainException("Snapshot job does not exist: " + jobId));
        entity.markFailed(errorMessage);
    }
}
