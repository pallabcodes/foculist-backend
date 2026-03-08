package com.yourorg.platform.foculist.meeting.clean.domain.model;

import com.yourorg.platform.foculist.tenancy.domain.DomainException;

public final class MeetingDomainException extends DomainException {
    public MeetingDomainException(String message) {
        super("MEETING_DOMAIN_ERROR", message);
    }
}
