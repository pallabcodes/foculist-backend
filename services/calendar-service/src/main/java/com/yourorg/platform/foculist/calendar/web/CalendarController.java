package com.yourorg.platform.foculist.calendar.web;

import com.yourorg.platform.foculist.calendar.application.AgendaContextView;
import com.yourorg.platform.foculist.calendar.application.CalendarApplicationService;
import com.yourorg.platform.foculist.calendar.application.CalendarEventView;
import com.yourorg.platform.foculist.calendar.application.CreateCalendarEventCommand;
import com.yourorg.platform.foculist.calendar.application.UpsertAgendaContextCommand;
import com.yourorg.platform.foculist.tenancy.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.UUID;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/calendar")
@Validated
public class CalendarController {
    private final CalendarApplicationService calendarApplicationService;

    public CalendarController(CalendarApplicationService calendarApplicationService) {
        this.calendarApplicationService = calendarApplicationService;
    }

    @GetMapping("/events")
    public List<CalendarEventView> listEvents() {
        return calendarApplicationService.listEvents(TenantContext.require());
    }

    @PostMapping("/events")
    public ResponseEntity<CalendarEventView> createEvent(@Valid @RequestBody CalendarEventRequest request) {
        CalendarEventView created = calendarApplicationService.createEvent(
                TenantContext.require(),
                new CreateCalendarEventCommand(request.title(), request.date(), request.time())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/events/{eventId}")
    public ResponseEntity<CalendarEventView> updateEvent(@PathVariable UUID eventId, @Valid @RequestBody UpdateCalendarEventRequest request) {
        CalendarEventView updated = calendarApplicationService.updateEvent(
                TenantContext.require(),
                eventId,
                new com.yourorg.platform.foculist.calendar.application.UpdateCalendarEventCommand(request.title(), request.date(), request.time())
        );
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/events/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID eventId) {
        calendarApplicationService.deleteEvent(TenantContext.require(), eventId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/agenda-context")
    public AgendaContextView setAgendaContext(@Valid @RequestBody AgendaContextRequest request) {
        return calendarApplicationService.upsertAgendaContext(
                TenantContext.require(),
                new UpsertAgendaContextCommand(request.meetingId(), request.title(), request.startTime())
        );
    }

    @DeleteMapping("/agenda-context/{agendaContextId}")
    public ResponseEntity<Void> deleteAgendaContext(@PathVariable UUID agendaContextId) {
        calendarApplicationService.deleteAgendaContext(TenantContext.require(), agendaContextId);
        return ResponseEntity.noContent().build();
    }

    public record CalendarEventRequest(@NotBlank String title, @NotBlank String date, @NotBlank String time) {
    }

    public record AgendaContextRequest(@NotBlank String meetingId, @NotBlank String title, String startTime) {
    }

    public record UpdateCalendarEventRequest(@NotBlank String title, @NotBlank String date, @NotBlank String time) {}
}
