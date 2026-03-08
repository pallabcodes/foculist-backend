package com.yourorg.platform.foculist.planning.domain.model;

public enum OutboxEventStatus {
    NEW,
    PROCESSING,
    PUBLISHED,
    FAILED
}
