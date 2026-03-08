package com.yourorg.platform.foculist.calendar.domain.model;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public record AgendaContext(
        UUID id,
        String tenantId,
        String meetingId,
        String title,
        LocalTime startTime,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public AgendaContext {
        if (id == null) {
            throw new CalendarDomainException("Agenda context id is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new CalendarDomainException("Agenda context tenantId is required");
        }
        if (meetingId == null || meetingId.isBlank()) {
            throw new CalendarDomainException("Agenda context meetingId is required");
        }
        if (title == null || title.isBlank()) {
            throw new CalendarDomainException("Agenda context title is required");
        }
        if (createdAt == null || updatedAt == null) {
            throw new CalendarDomainException("Agenda context timestamps are required");
        }
        if (updatedAt.isBefore(createdAt)) {
            throw new CalendarDomainException("Agenda context updatedAt cannot be before createdAt");
        }
        if (version < 0) {
            throw new CalendarDomainException("Agenda context version cannot be negative");
        }

        tenantId = tenantId.trim();
        meetingId = meetingId.trim();
        title = title.trim();
    }

    public static AgendaContext create(
            String tenantId,
            String meetingId,
            String title,
            LocalTime startTime,
            Instant now
    ) {
        Instant timestamp = now == null ? Instant.now() : now;
        return new AgendaContext(
                UUID.randomUUID(),
                tenantId,
                meetingId,
                title,
                startTime,
                timestamp,
                timestamp,
                0L
        );
    }

    public AgendaContext update(String title, LocalTime startTime, Instant now) {
        return new AgendaContext(
                id,
                tenantId,
                meetingId,
                title == null || title.isBlank() ? this.title : title,
                startTime == null ? this.startTime : startTime,
                createdAt,
                now == null ? Instant.now() : now,
                version
        );
    }
}
