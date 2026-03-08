package com.yourorg.platform.foculist.meeting.clean.domain.model;

public record ExtractedTask(
        String title,
        TaskPriority priority,
        String sourceMeetingId,
        String tenantId
) {
    public ExtractedTask {
        if (title == null || title.isBlank()) {
            throw new MeetingDomainException("Extracted task title is required");
        }
        if (priority == null) {
            throw new MeetingDomainException("Extracted task priority is required");
        }
        if (sourceMeetingId == null || sourceMeetingId.isBlank()) {
            throw new MeetingDomainException("Extracted task sourceMeetingId is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new MeetingDomainException("Extracted task tenantId is required");
        }
        title = title.trim();
        sourceMeetingId = sourceMeetingId.trim();
        tenantId = tenantId.trim();
    }
}
