package com.yourorg.platform.foculist.calendar.application;

public record UpsertAgendaContextCommand(
        String meetingId,
        String title,
        String startTime
) {
}
