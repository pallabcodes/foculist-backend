package com.yourorg.platform.foculist.project.application;

public record CreateProjectCommand(
        String name,
        String description,
        String status,
        String priority,
        String dueDate
) {
}
