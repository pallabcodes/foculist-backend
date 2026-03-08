package com.yourorg.platform.foculist.project.application;

import java.time.LocalDate;
import java.util.UUID;

public record ProjectSummaryView(
        UUID id,
        String name,
        String description,
        String status,
        String priority,
        LocalDate dueDate,
        String tenantId
) {
}
