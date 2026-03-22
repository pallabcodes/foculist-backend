package com.yourorg.platform.foculist.planning.application;

public record CreateBoardColumnCommand(
        String name,
        String statusMapping,
        Integer orderIndex
) {}
