package com.yourorg.platform.foculist.calendar.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.calendar.domain.model.AgendaContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(
        name = "calendar_agenda_context",
        indexes = {
                @Index(name = "idx_calendar_agenda_tenant_meeting", columnList = "tenant_id,meeting_id", unique = true)
        }
)
public class AgendaContextJpaEntity {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 128)
    private String tenantId;

    @Column(name = "meeting_id", nullable = false, length = 128)
    private String meetingId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected AgendaContextJpaEntity() {
    }

    private AgendaContextJpaEntity(
            UUID id,
            String tenantId,
            String meetingId,
            String title,
            LocalTime startTime,
            Instant createdAt,
            Instant updatedAt,
            long version
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.meetingId = meetingId;
        this.title = title;
        this.startTime = startTime;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static AgendaContextJpaEntity fromDomain(AgendaContext agendaContext) {
        return new AgendaContextJpaEntity(
                agendaContext.id(),
                agendaContext.tenantId(),
                agendaContext.meetingId(),
                agendaContext.title(),
                agendaContext.startTime(),
                agendaContext.createdAt(),
                agendaContext.updatedAt(),
                agendaContext.version()
        );
    }

    public AgendaContext toDomain() {
        return new AgendaContext(
                id,
                tenantId,
                meetingId,
                title,
                startTime,
                createdAt,
                updatedAt,
                version
        );
    }
}
