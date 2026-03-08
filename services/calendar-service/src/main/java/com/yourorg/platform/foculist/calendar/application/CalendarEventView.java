package com.yourorg.platform.foculist.calendar.application;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CalendarEventView(
        UUID id,
        String title,
        LocalDate date,
        LocalTime time,
        String tenantId
) {
}
