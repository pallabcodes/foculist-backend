package com.yourorg.platform.foculist.calendar.application;

public record CreateCalendarEventCommand(
        String title,
        String date,
        String time
) {
}
