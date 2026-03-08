package com.yourorg.platform.foculist.planning.domain.model;

import java.time.Instant;
import java.util.UUID;

public record TaskSnapshotJob(
        UUID id,
        String tenantId,
        UUID taskId,
        long targetVersion,
        OutboxEventStatus status,
        int attempts,
        String lastError,
        Instant createdAt,
        Instant processedAt
) {
    public TaskSnapshotJob {
        if (id == null) {
            throw new PlanningDomainException("Snapshot job id is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new PlanningDomainException("Snapshot job tenant id is required");
        }
        if (taskId == null) {
            throw new PlanningDomainException("Snapshot job task id is required");
        }
        if (targetVersion < 0) {
            throw new PlanningDomainException("Snapshot job target version cannot be negative");
        }
        if (status == null) {
            throw new PlanningDomainException("Snapshot job status is required");
        }
        if (createdAt == null) {
            throw new PlanningDomainException("Snapshot job createdAt is required");
        }
    }

    public static TaskSnapshotJob pending(String tenantId, UUID taskId, long targetVersion, Instant createdAt) {
        return new TaskSnapshotJob(
                UUID.randomUUID(),
                tenantId,
                taskId,
                targetVersion,
                OutboxEventStatus.NEW,
                0,
                null,
                createdAt,
                null
        );
    }
}
