package com.yourorg.platform.foculist.project.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProjectSettingsView(
        UUID projectId,
        List<String> workflowStatuses,
        String defaultView,
        String tenantId,
        Instant updatedAt
) {
}
