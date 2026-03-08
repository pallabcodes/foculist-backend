package com.yourorg.platform.foculist.meeting.clean.application.view;

import java.time.Instant;
import java.util.UUID;

public record MeetingSummaryView(
        UUID id,
        String meetingId,
        String content,
        String style,
        String tenantId,
        Instant createdAt
) {
}
