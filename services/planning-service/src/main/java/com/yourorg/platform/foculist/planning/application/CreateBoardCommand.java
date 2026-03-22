package com.yourorg.platform.foculist.planning.application;

import java.util.UUID;

public record CreateBoardCommand(
        UUID projectId,
        String name,
        String type
) {}
