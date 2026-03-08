package com.yourorg.platform.foculist.planning.application;

public record UpdateTaskCommand(String sprintId, String title, String description, String priority) {
}
