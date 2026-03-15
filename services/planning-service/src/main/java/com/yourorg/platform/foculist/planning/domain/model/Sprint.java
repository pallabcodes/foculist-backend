package com.yourorg.platform.foculist.planning.domain.model;

import java.time.Instant;
import java.util.UUID;

import java.util.Map;

public record Sprint(
        UUID id,
        String tenantId,
        String name,
        SprintStatus status,
        Instant startDate,
        Instant endDate,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy,
        Instant deletedAt,
        Map<String, Object> metadata,
        Long version
) {
    public Sprint {
        if (id == null) {
            throw new PlanningDomainException("Sprint id is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new PlanningDomainException("Tenant id is required");
        }
        if (name == null || name.isBlank()) {
            throw new PlanningDomainException("Sprint name is required");
        }
        if (status == null) {
            throw new PlanningDomainException("Sprint status is required");
        }
        if (startDate == null || endDate == null) {
            throw new PlanningDomainException("Sprint dates are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new PlanningDomainException("Sprint end date must be after start date");
        }
        if (createdAt == null || updatedAt == null) {
            throw new PlanningDomainException("Sprint timestamps are required");
        }
    }

    public Sprint update(String name, Instant startDate, Instant endDate, Instant now, String updatedBy) {
        return new Sprint(id, tenantId, name, status, startDate, endDate, createdAt, now, createdBy, updatedBy, deletedAt, metadata, version);
    }
}
