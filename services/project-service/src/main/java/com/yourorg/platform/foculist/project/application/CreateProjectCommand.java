package com.yourorg.platform.foculist.project.application;

import java.util.UUID;

public record CreateProjectCommand(
        String name,
        String description,
        String status,
        String priority,
        String dueDate,
        UUID ownerId,
        String key,
        UUID permissionSchemeId
) {
}
