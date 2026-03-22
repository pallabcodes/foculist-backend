package com.yourorg.platform.foculist.project.application;

import java.time.LocalDate;
import java.util.UUID;

public record CreateTaskCommand(
        String title,
        String description,
        String status,
        String priority,
        String type,
        Integer storyPoints,
        UUID assigneeId,
        LocalDate dueDate
) {
}
