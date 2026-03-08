package com.yourorg.platform.foculist.project.application;

import java.util.List;

public record UpdateProjectSettingsCommand(
        List<String> workflowStatuses,
        String defaultView
) {
}
