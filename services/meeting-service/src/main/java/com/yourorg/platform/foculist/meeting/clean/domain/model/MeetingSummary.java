package com.yourorg.platform.foculist.meeting.clean.domain.model;

import java.time.Instant;
import java.util.UUID;

public record MeetingSummary(
        UUID id,
        String tenantId,
        String meetingId,
        String content,
        SummaryStyle style,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public MeetingSummary {
        if (id == null) {
            throw new MeetingDomainException("Meeting summary id is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new MeetingDomainException("Meeting summary tenantId is required");
        }
        if (meetingId == null || meetingId.isBlank()) {
            throw new MeetingDomainException("Meeting summary meetingId is required");
        }
        if (content == null || content.isBlank()) {
            throw new MeetingDomainException("Meeting summary content is required");
        }
        if (style == null) {
            throw new MeetingDomainException("Meeting summary style is required");
        }
        if (createdAt == null || updatedAt == null) {
            throw new MeetingDomainException("Meeting summary timestamps are required");
        }
        if (updatedAt.isBefore(createdAt)) {
            throw new MeetingDomainException("Meeting summary updatedAt cannot be before createdAt");
        }
        if (version < 0) {
            throw new MeetingDomainException("Meeting summary version cannot be negative");
        }
        if (content.length() > 20000) {
            throw new MeetingDomainException("Meeting summary content is too long");
        }

        tenantId = tenantId.trim();
        meetingId = meetingId.trim();
        content = content.trim();
    }

    public static MeetingSummary create(
            String tenantId,
            String meetingId,
            String content,
            SummaryStyle style,
            Instant now
    ) {
        Instant timestamp = now == null ? Instant.now() : now;
        return new MeetingSummary(
                UUID.randomUUID(),
                tenantId,
                meetingId,
                content,
                style == null ? SummaryStyle.CONCISE : style,
                timestamp,
                timestamp,
                0L
        );
    }
}
