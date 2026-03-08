package com.yourorg.platform.foculist.meeting.clean.application.command;

public record CreateMeetingSummaryCommand(
        String meetingId,
        String content,
        String style
) {
}
