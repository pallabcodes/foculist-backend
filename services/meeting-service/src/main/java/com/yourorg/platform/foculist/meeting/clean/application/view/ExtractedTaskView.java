package com.yourorg.platform.foculist.meeting.clean.application.view;

public record ExtractedTaskView(
        String title,
        String priority,
        String sourceMeetingId,
        String tenantId
) {
}
