package com.yourorg.platform.foculist.calendar.domain.port;

import com.yourorg.platform.foculist.calendar.domain.model.CalendarEvent;
import java.util.List;

public interface CalendarEventRepositoryPort {
    List<CalendarEvent> findByTenantId(String tenantId);

    java.util.Optional<CalendarEvent> findByIdAndTenantId(java.util.UUID id, String tenantId);

    CalendarEvent save(CalendarEvent calendarEvent);

    void delete(CalendarEvent calendarEvent);
}
