package com.yourorg.platform.foculist.project.domain.model;

import java.util.Locale;

public enum ProjectDefaultView {
    BOARD,
    LIST,
    TIMELINE,
    CALENDAR;

    public static ProjectDefaultView fromOrDefault(String rawDefaultView) {
        if (rawDefaultView == null || rawDefaultView.isBlank()) {
            return BOARD;
        }
        return from(rawDefaultView);
    }

    public static ProjectDefaultView fromNullable(String rawDefaultView) {
        if (rawDefaultView == null || rawDefaultView.isBlank()) {
            return null;
        }
        return from(rawDefaultView);
    }

    private static ProjectDefaultView from(String rawDefaultView) {
        try {
            return valueOf(rawDefaultView.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ProjectDomainException("Invalid project default view: " + rawDefaultView);
        }
    }
}
