package com.yourorg.platform.foculist.calendar.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.calendar.domain.model.CalendarEvent;
import com.yourorg.platform.foculist.calendar.domain.port.CalendarEventRepositoryPort;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class JpaCalendarEventRepositoryAdapter implements CalendarEventRepositoryPort {
    private final CalendarEventJpaRepository calendarEventRepository;

    public JpaCalendarEventRepositoryAdapter(CalendarEventJpaRepository calendarEventRepository) {
        this.calendarEventRepository = calendarEventRepository;
    }

    @Override
    public List<CalendarEvent> findByTenantId(String tenantId) {
        return calendarEventRepository.findByTenantIdOrderByDateAscTimeAsc(tenantId).stream()
                .map(CalendarEventJpaEntity::toDomain)
                .toList();
    }

    @Override
    public java.util.Optional<CalendarEvent> findByIdAndTenantId(java.util.UUID id, String tenantId) {
        return calendarEventRepository.findByIdAndTenantId(id, tenantId)
                .map(CalendarEventJpaEntity::toDomain);
    }

    @Override
    public CalendarEvent save(CalendarEvent calendarEvent) {
        return calendarEventRepository.save(CalendarEventJpaEntity.fromDomain(calendarEvent)).toDomain();
    }

    @Override
    public void delete(CalendarEvent calendarEvent) {
        calendarEventRepository.delete(CalendarEventJpaEntity.fromDomain(calendarEvent));
    }
}
