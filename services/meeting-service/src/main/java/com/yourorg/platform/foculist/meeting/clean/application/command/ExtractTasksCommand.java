package com.yourorg.platform.foculist.meeting.clean.application.command;

public record ExtractTasksCommand(
        String meetingId,
        String transcript
) {
}
