package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.planning.domain.model.Sprint;
import com.yourorg.platform.foculist.planning.domain.model.SprintStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "planning_sprint",
        indexes = {
                @Index(name = "idx_planning_sprint_tenant", columnList = "tenant_id"),
                @Index(name = "idx_planning_sprint_tenant_status", columnList = "tenant_id,status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_planning_sprint_tenant_name", columnNames = {"tenant_id", "name"})
        }
)
public class SprintJpaEntity {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 128)
    private String tenantId;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SprintStatus status;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "end_date", nullable = false)
    private Instant endDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected SprintJpaEntity() {
    }

    SprintJpaEntity(
            UUID id,
            String tenantId,
            String name,
            SprintStatus status,
            Instant startDate,
            Instant endDate,
            Instant createdAt,
            Instant updatedAt,
            long version
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static SprintJpaEntity fromDomain(Sprint sprint) {
        return new SprintJpaEntity(
                sprint.id(),
                sprint.tenantId(),
                sprint.name(),
                sprint.status(),
                sprint.startDate(),
                sprint.endDate(),
                sprint.createdAt(),
                sprint.updatedAt(),
                sprint.version() != null ? sprint.version() : 0L
        );
    }

    public Sprint toDomain() {
        return new Sprint(
                id,
                tenantId,
                name,
                status,
                startDate,
                endDate,
                createdAt,
                updatedAt,
                version
        );
    }
}
