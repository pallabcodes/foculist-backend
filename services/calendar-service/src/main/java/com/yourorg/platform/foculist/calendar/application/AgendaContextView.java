package com.yourorg.platform.foculist.calendar.application;

import java.time.LocalTime;
import java.util.UUID;

public record AgendaContextView(
        UUID id,
        String meetingId,
        String title,
        LocalTime startTime,
        String tenantId
) {
}
