package com.yourorg.platform.foculist.planning.application;

public record UpdateTaskPlanningCommand(
        String sprintId,
        String epicId,
        String boardColumnId
) {}
