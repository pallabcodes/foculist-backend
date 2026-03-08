package com.yourorg.platform.foculist.calendar.domain.model;

import com.yourorg.platform.foculist.tenancy.domain.DomainException;

public final class CalendarDomainException extends DomainException {
    public CalendarDomainException(String message) {
        super("CALENDAR_DOMAIN_ERROR", message);
    }
}
