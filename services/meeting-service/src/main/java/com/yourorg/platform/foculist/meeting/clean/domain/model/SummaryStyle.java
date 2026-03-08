package com.yourorg.platform.foculist.meeting.clean.domain.model;

import java.util.Locale;

public enum SummaryStyle {
    CONCISE,
    DETAILED,
    ACTION_FOCUSED;

    public static SummaryStyle from(String rawStyle) {
        if (rawStyle == null || rawStyle.isBlank()) {
            return CONCISE;
        }
        try {
            return valueOf(rawStyle.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException ex) {
            throw new MeetingDomainException("Invalid summary style: " + rawStyle);
        }
    }
}
