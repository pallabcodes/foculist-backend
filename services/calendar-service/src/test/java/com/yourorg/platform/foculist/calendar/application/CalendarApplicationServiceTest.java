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
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CalendarApplicationServiceTest {

    @Test
    void createsCalendarEvent() {
        Instant now = Instant.parse("2026-02-03T09:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        CalendarEventRepositoryPort eventRepository = mock(CalendarEventRepositoryPort.class);
        AgendaContextRepositoryPort agendaRepository = mock(AgendaContextRepositoryPort.class);
        CalendarApplicationService service = new CalendarApplicationService(eventRepository, agendaRepository, clock);

        when(eventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CalendarEventView created = service.createEvent(
                "tenant-a",
                new CreateCalendarEventCommand("Sprint Planning", "2026-02-10", "09:30")
        );

        assertThat(created.tenantId()).isEqualTo("tenant-a");
        assertThat(created.title()).isEqualTo("Sprint Planning");
        assertThat(created.date()).isEqualTo(LocalDate.parse("2026-02-10"));
        assertThat(created.time()).isEqualTo(LocalTime.parse("09:30"));
    }

    @Test
    void rejectsInvalidDateFormat() {
        CalendarEventRepositoryPort eventRepository = mock(CalendarEventRepositoryPort.class);
        AgendaContextRepositoryPort agendaRepository = mock(AgendaContextRepositoryPort.class);
        CalendarApplicationService service = new CalendarApplicationService(eventRepository, agendaRepository);

        assertThatThrownBy(() -> service.createEvent(
                "tenant-a",
                new CreateCalendarEventCommand("Sprint Planning", "10-02-2026", "09:30")
        )).isInstanceOf(CalendarDomainException.class).hasMessageContaining("yyyy-MM-dd");
    }

    @Test
    void listsEventsForTenant() {
        CalendarEventRepositoryPort eventRepository = mock(CalendarEventRepositoryPort.class);
        AgendaContextRepositoryPort agendaRepository = mock(AgendaContextRepositoryPort.class);
        CalendarApplicationService service = new CalendarApplicationService(eventRepository, agendaRepository);

        CalendarEvent event = CalendarEvent.create(
                "tenant-a",
                "Roadmap Review",
                LocalDate.parse("2026-02-11"),
                LocalTime.parse("14:00"),
                Instant.parse("2026-02-03T09:00:00Z")
        );
        when(eventRepository.findByTenantId("tenant-a")).thenReturn(List.of(event));

        List<CalendarEventView> events = service.listEvents("tenant-a");
        assertThat(events).hasSize(1);
        assertThat(events.get(0).title()).isEqualTo("Roadmap Review");
    }

    @Test
    void upsertsAgendaContextByMeetingIdAndTenant() {
        Instant now = Instant.parse("2026-02-03T09:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        CalendarEventRepositoryPort eventRepository = mock(CalendarEventRepositoryPort.class);
        AgendaContextRepositoryPort agendaRepository = mock(AgendaContextRepositoryPort.class);
        CalendarApplicationService service = new CalendarApplicationService(eventRepository, agendaRepository, clock);

        AgendaContext existing = AgendaContext.create(
                "tenant-a",
                "meeting-1",
                "Daily Sync",
                LocalTime.parse("09:00"),
                now.minusSeconds(120)
        );
        when(agendaRepository.findByTenantIdAndMeetingId("tenant-a", "meeting-1"))
                .thenReturn(Optional.of(existing));
        when(agendaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AgendaContextView updated = service.upsertAgendaContext(
                "tenant-a",
                new UpsertAgendaContextCommand("meeting-1", "Daily Sync - Updated", "09:15")
        );

        assertThat(updated.title()).isEqualTo("Daily Sync - Updated");
        assertThat(updated.startTime()).isEqualTo(LocalTime.parse("09:15"));
        assertThat(updated.tenantId()).isEqualTo("tenant-a");
    }

    @Test
    void createsAgendaContextWhenMissing() {
        CalendarEventRepositoryPort eventRepository = mock(CalendarEventRepositoryPort.class);
        AgendaContextRepositoryPort agendaRepository = mock(AgendaContextRepositoryPort.class);
        CalendarApplicationService service = new CalendarApplicationService(eventRepository, agendaRepository);

        when(agendaRepository.findByTenantIdAndMeetingId("tenant-a", "meeting-2")).thenReturn(Optional.empty());
        when(agendaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AgendaContextView created = service.upsertAgendaContext(
                "tenant-a",
                new UpsertAgendaContextCommand("meeting-2", "Kickoff", null)
        );

        assertThat(created.meetingId()).isEqualTo("meeting-2");
        assertThat(created.startTime()).isNull();
    }
}
