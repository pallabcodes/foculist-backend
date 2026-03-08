package com.yourorg.platform.foculist.resource.clean.domain.model;

import java.time.Instant;
import java.util.UUID;

public record WorklogEntry(
        UUID id,
        String tenantId,
        String project,
        String task,
        int durationMinutes,
        Instant loggedAt,
        Instant createdAt,
        long version
) {
    public WorklogEntry {
        if (id == null) {
            throw new ResourceDomainException("Worklog id is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new ResourceDomainException("Worklog tenantId is required");
        }
        if (project == null || project.isBlank()) {
            throw new ResourceDomainException("Worklog project is required");
        }
        if (task == null || task.isBlank()) {
            throw new ResourceDomainException("Worklog task is required");
        }
        if (durationMinutes <= 0 || durationMinutes > 24 * 60) {
            throw new ResourceDomainException("Worklog durationMinutes must be between 1 and 1440");
        }
        if (loggedAt == null || createdAt == null) {
            throw new ResourceDomainException("Worklog timestamps are required");
        }
        if (version < 0) {
            throw new ResourceDomainException("Worklog version cannot be negative");
        }

        tenantId = tenantId.trim();
        project = project.trim();
        task = task.trim();
    }

    public static WorklogEntry create(
            String tenantId,
            String project,
            String task,
            int durationMinutes,
            Instant now
    ) {
        Instant timestamp = now == null ? Instant.now() : now;
        return new WorklogEntry(
                UUID.randomUUID(),
                tenantId,
                project,
                task,
                durationMinutes,
                timestamp,
                timestamp,
                0L
        );
    }

    public WorklogEntry update(String project, String task, int durationMinutes) {
        return new WorklogEntry(id, tenantId, project, task, durationMinutes, loggedAt, createdAt, version);
    }
}
