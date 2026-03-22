package com.yourorg.platform.foculist.calendar.application;

import com.yourorg.platform.foculist.calendar.domain.model.AgendaContext;
import com.yourorg.platform.foculist.calendar.domain.model.CalendarDomainException;
import com.yourorg.platform.foculist.calendar.domain.model.CalendarEvent;
import com.yourorg.platform.foculist.calendar.domain.port.AgendaContextRepositoryPort;
import com.yourorg.platform.foculist.calendar.domain.port.CalendarEventRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalendarApplicationService {
    private final CalendarEventRepositoryPort calendarEventRepository;
    private final AgendaContextRepositoryPort agendaContextRepository;
    private final Clock clock;

    public CalendarApplicationService(
            CalendarEventRepositoryPort calendarEventRepository,
            AgendaContextRepositoryPort agendaContextRepository
    ) {
        this.calendarEventRepository = calendarEventRepository;
        this.agendaContextRepository = agendaContextRepository;
        this.clock = Clock.systemUTC();
    }

    @Transactional(readOnly = true)
    public List<CalendarEventView> listEvents(String tenantId) {
        return calendarEventRepository.findByTenantId(tenantId).stream()
                .map(this::toEventView)
                .toList();
    }

    @Transactional
    public CalendarEventView createEvent(String tenantId, CreateCalendarEventCommand command) {
        CalendarEvent created = calendarEventRepository.save(CalendarEvent.create(
                tenantId,
                command.title(),
                parseDate(command.date()),
                parseTime(command.time(), "time"),
                Instant.now(clock)
        ));
        return toEventView(created);
    }

    @Transactional
    public CalendarEventView updateEvent(String tenantId, java.util.UUID eventId, UpdateCalendarEventCommand command) {
        CalendarEvent event = calendarEventRepository.findByIdAndTenantId(eventId, tenantId)
                .orElseThrow(() -> new CalendarDomainException("Calendar event not found"));
        CalendarEvent updated = event.update(
                command.title(),
                parseDate(command.date()),
                parseTime(command.time(), "time"),
                Instant.now(clock)
        );
        return toEventView(calendarEventRepository.save(updated));
    }

    @Transactional
    public void deleteEvent(String tenantId, java.util.UUID eventId) {
        CalendarEvent event = calendarEventRepository.findByIdAndTenantId(eventId, tenantId)
                .orElseThrow(() -> new CalendarDomainException("Calendar event not found"));
        calendarEventRepository.delete(event);
    }

    @Transactional
    public AgendaContextView upsertAgendaContext(String tenantId, UpsertAgendaContextCommand command) {
        LocalTime startTime = parseOptionalTime(command.startTime(), "startTime");
        Instant now = Instant.now(clock);
        AgendaContext persisted = agendaContextRepository.findByTenantIdAndMeetingId(tenantId, command.meetingId())
                .map(existing -> existing.update(command.title(), startTime, now))
                .orElseGet(() -> AgendaContext.create(
                        tenantId,
                        command.meetingId(),
                        command.title(),
                        startTime,
                        now
                ));
        return toAgendaView(agendaContextRepository.save(persisted));
    }

    @Transactional
    public void deleteAgendaContext(String tenantId, java.util.UUID agendaContextId) {
        AgendaContext context = agendaContextRepository.findByIdAndTenantId(agendaContextId, tenantId)
                .orElseThrow(() -> new CalendarDomainException("Agenda context not found"));
        agendaContextRepository.delete(context);
    }

    private LocalDate parseDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            throw new CalendarDomainException("date is required");
        }
        try {
            return LocalDate.parse(rawDate.trim());
        } catch (DateTimeParseException ex) {
            throw new CalendarDomainException("Invalid date format. Expected yyyy-MM-dd");
        }
    }

    private LocalTime parseTime(String rawTime, String fieldName) {
        if (rawTime == null || rawTime.isBlank()) {
            throw new CalendarDomainException(fieldName + " is required");
        }
        try {
            return LocalTime.parse(rawTime.trim());
        } catch (DateTimeParseException ex) {
            throw new CalendarDomainException("Invalid " + fieldName + " format. Expected HH:mm or HH:mm:ss");
        }
    }

    private LocalTime parseOptionalTime(String rawTime, String fieldName) {
        if (rawTime == null || rawTime.isBlank()) {
            return null;
        }
        return parseTime(rawTime, fieldName);
    }

    private CalendarEventView toEventView(CalendarEvent calendarEvent) {
        return new CalendarEventView(
                calendarEvent.id(),
                calendarEvent.title(),
                calendarEvent.date(),
                calendarEvent.time(),
                calendarEvent.tenantId()
        );
    }

    private AgendaContextView toAgendaView(AgendaContext agendaContext) {
        return new AgendaContextView(
                agendaContext.id(),
                agendaContext.meetingId(),
                agendaContext.title(),
                agendaContext.startTime(),
                agendaContext.tenantId()
        );
    }
}
