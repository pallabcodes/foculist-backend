package com.yourorg.platform.foculist.project.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import java.util.Map;

public record ProjectSettings(
        UUID projectId,
        String tenantId,
        List<String> workflowStatuses,
        ProjectDefaultView defaultView,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy,
        Map<String, Object> metadata,
        long version
) {
    private static final List<String> DEFAULT_WORKFLOW_STATUSES = List.of("TODO", "IN_PROGRESS", "REVIEW", "DONE");

    public ProjectSettings {
        if (projectId == null) {
            throw new ProjectDomainException("Project settings projectId is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new ProjectDomainException("Project settings tenantId is required");
        }
        if (defaultView == null) {
            throw new ProjectDomainException("Project settings defaultView is required");
        }
        if (createdAt == null || updatedAt == null) {
            throw new ProjectDomainException("Project settings timestamps are required");
        }
        if (version < 0) {
            throw new ProjectDomainException("Project settings version cannot be negative");
        }

        tenantId = tenantId.trim();
        workflowStatuses = normalizeWorkflowStatuses(workflowStatuses);
    }

    public static ProjectSettings createDefault(UUID projectId, String tenantId, Instant now, String createdBy) {
        Instant timestamp = now == null ? Instant.now() : now;
        return new ProjectSettings(
                projectId,
                tenantId,
                DEFAULT_WORKFLOW_STATUSES,
                ProjectDefaultView.BOARD,
                timestamp,
                timestamp,
                createdBy,
                null,
                null,
                0L
        );
    }

    public ProjectSettings update(
            List<String> requestedWorkflowStatuses,
            ProjectDefaultView requestedDefaultView,
            Instant now,
            String updatedBy
    ) {
        List<String> statuses = requestedWorkflowStatuses == null
                ? workflowStatuses
                : normalizeWorkflowStatuses(requestedWorkflowStatuses);
        ProjectDefaultView resolvedDefaultView = requestedDefaultView == null ? defaultView : requestedDefaultView;
        return new ProjectSettings(
                projectId,
                tenantId,
                statuses,
                resolvedDefaultView,
                createdAt,
                now == null ? Instant.now() : now,
                createdBy,
                updatedBy,
                metadata,
                version
        );
    }

    private static List<String> normalizeWorkflowStatuses(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            throw new ProjectDomainException("At least one workflow status is required");
        }
        Set<String> deduplicated = new LinkedHashSet<>();
        for (String status : statuses) {
            if (status == null || status.isBlank()) {
                continue;
            }
            String normalized = status.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
            if (normalized.length() > 64) {
                throw new ProjectDomainException("Workflow status is too long");
            }
            deduplicated.add(normalized);
        }
        if (deduplicated.isEmpty()) {
            throw new ProjectDomainException("At least one workflow status is required");
        }
        if (deduplicated.size() > 20) {
            throw new ProjectDomainException("Too many workflow statuses");
        }
        return List.copyOf(new ArrayList<>(deduplicated));
    }
}
