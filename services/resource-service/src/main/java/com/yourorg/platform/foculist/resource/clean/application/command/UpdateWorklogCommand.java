package com.yourorg.platform.foculist.resource.clean.application.command;

public record UpdateWorklogCommand(String project, String task, int durationMinutes) {
}
