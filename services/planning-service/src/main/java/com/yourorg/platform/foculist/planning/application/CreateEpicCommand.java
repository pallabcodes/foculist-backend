package com.yourorg.platform.foculist.planning.application;

import java.util.UUID;

public record CreateEpicCommand(
        UUID projectId,
        String name,
        String summary,
        String color,
        String startDate,
        String targetDate
) {}
