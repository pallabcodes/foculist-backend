package com.yourorg.platform.foculist.resource.clean.application.command;

public record CreateBookmarkCommand(
        String title,
        String url
) {
}
