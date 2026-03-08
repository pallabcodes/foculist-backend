package com.yourorg.platform.foculist.calendar.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.calendar.domain.model.CalendarEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(
        name = "calendar_event",
        indexes = {
                @Index(name = "idx_calendar_event_tenant_date_time", columnList = "tenant_id,event_date,event_time")
        }
)
public class CalendarEventJpaEntity {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 128)
    private String tenantId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "event_date", nullable = false)
    private LocalDate date;

    @Column(name = "event_time", nullable = false)
    private LocalTime time;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected CalendarEventJpaEntity() {
    }

    private CalendarEventJpaEntity(
            UUID id,
            String tenantId,
            String title,
            LocalDate date,
            LocalTime time,
            Instant createdAt,
            Instant updatedAt,
            long version
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.title = title;
        this.date = date;
        this.time = time;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static CalendarEventJpaEntity fromDomain(CalendarEvent calendarEvent) {
        return new CalendarEventJpaEntity(
                calendarEvent.id(),
                calendarEvent.tenantId(),
                calendarEvent.title(),
                calendarEvent.date(),
                calendarEvent.time(),
                calendarEvent.createdAt(),
                calendarEvent.updatedAt(),
                calendarEvent.version()
        );
    }

    public CalendarEvent toDomain() {
        return new CalendarEvent(
                id,
                tenantId,
                title,
                date,
                time,
                createdAt,
                updatedAt,
                version
        );
    }
}
