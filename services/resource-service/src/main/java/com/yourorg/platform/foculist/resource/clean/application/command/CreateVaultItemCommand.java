package com.yourorg.platform.foculist.resource.clean.application.command;

public record CreateVaultItemCommand(
        String name,
        String classification
) {
}
