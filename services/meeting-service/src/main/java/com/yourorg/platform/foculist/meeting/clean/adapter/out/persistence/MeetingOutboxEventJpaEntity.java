package com.yourorg.platform.foculist.meeting.clean.adapter.out.persistence;

import com.yourorg.platform.foculist.meeting.clean.domain.model.MeetingOutboxEventStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "meeting_outbox_events", schema = "meeting")
@Getter
@Setter
@NoArgsConstructor
public class MeetingOutboxEventJpaEntity {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(columnDefinition = "text", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingOutboxEventStatus status;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    private int attempts;

    @Column(name = "last_error")
    private String lastError;
}
