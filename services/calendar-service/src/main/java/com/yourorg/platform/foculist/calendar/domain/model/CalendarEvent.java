package com.yourorg.platform.foculist.calendar.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CalendarEvent(
        UUID id,
        String tenantId,
        String title,
        LocalDate date,
        LocalTime time,
        Instant createdAt,
        Instant updatedAt,
        long version
) {

    public CalendarEvent {
        if (id == null) {
            throw new CalendarDomainException("Calendar event id is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new CalendarDomainException("Calendar event tenantId is required");
        }
        if (title == null || title.isBlank()) {
            throw new CalendarDomainException("Calendar event title is required");
        }
        if (date == null) {
            throw new CalendarDomainException("Calendar event date is required");
        }
        if (time == null) {
            throw new CalendarDomainException("Calendar event time is required");
        }
        if (createdAt == null || updatedAt == null) {
            throw new CalendarDomainException("Calendar event timestamps are required");
        }
        if (updatedAt.isBefore(createdAt)) {
            throw new CalendarDomainException("Calendar event updatedAt cannot be before createdAt");
        }
        if (version < 0) {
            throw new CalendarDomainException("Calendar event version cannot be negative");
        }

        tenantId = tenantId.trim();
        title = title.trim();
    }

    public static CalendarEvent create(
            String tenantId,
            String title,
            LocalDate date,
            LocalTime time,
            Instant now
    ) {
        Instant timestamp = now == null ? Instant.now() : now;
        return new CalendarEvent(
                UUID.randomUUID(),
                tenantId,
                title,
                date,
                time,
                timestamp,
                timestamp,
                0L
        );
    }

    public CalendarEvent update(String title, LocalDate date, LocalTime time, Instant now) {
        return new CalendarEvent(id, tenantId, title, date, time, createdAt, now, version);
    }
}
