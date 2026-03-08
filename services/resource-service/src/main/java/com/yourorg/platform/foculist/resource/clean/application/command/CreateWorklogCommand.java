package com.yourorg.platform.foculist.resource.clean.application.command;

public record CreateWorklogCommand(
        String project,
        String task,
        int durationMinutes
) {
}
