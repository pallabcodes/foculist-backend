package com.yourorg.platform.foculist.planning.application;

public record CreateTaskCommand(
        String title,
        String description,
        String status,
        String priority,
        String sprintId
) {
}
