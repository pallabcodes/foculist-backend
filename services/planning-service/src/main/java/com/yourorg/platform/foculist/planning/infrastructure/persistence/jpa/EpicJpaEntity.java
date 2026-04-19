package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.planning.domain.model.Epic;
import com.yourorg.platform.foculist.planning.domain.model.EpicStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "planning_epic",
        indexes = {
                @Index(name = "idx_planning_epic_tenant", columnList = "tenant_id"),
                @Index(name = "idx_planning_epic_tenant_project", columnList = "tenant_id,project_id"),
                @Index(name = "idx_planning_epic_deleted_at", columnList = "deleted_at")
        }
)
@SQLDelete(sql = "UPDATE planning_epic SET deleted_at = NOW() WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class EpicJpaEntity {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 128)
    private String tenantId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "summary", columnDefinition = "text")
    private String summary;

    @Column(name = "color", length = 32)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private EpicStatus status;

    @Column(name = "start_date")
    private Instant startDate;

    @Column(name = "target_date")
    private Instant targetDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", length = 128)
    private String createdBy;

    @Column(name = "updated_by", length = 128)
    private String updatedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected EpicJpaEntity() {
    }

    private EpicJpaEntity(
            UUID id,
            String tenantId,
            UUID projectId,
            String name,
            String summary,
            String color,
            EpicStatus status,
            Instant startDate,
            Instant targetDate,
            Instant createdAt,
            Instant updatedAt,
            String createdBy,
            String updatedBy,
            Instant deletedAt,
            Map<String, Object> metadata,
            long version
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.projectId = projectId;
        this.name = name;
        this.summary = summary;
        this.color = color;
        this.status = status;
        this.startDate = startDate;
        this.targetDate = targetDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.deletedAt = deletedAt;
        this.metadata = metadata;
        this.version = version;
    }

    public static EpicJpaEntity fromDomain(Epic epic) {
        return new EpicJpaEntity(
                epic.id(),
                epic.tenantId(),
                epic.projectId(),
                epic.name(),
                epic.summary(),
                epic.color(),
                epic.status(),
                epic.startDate(),
                epic.targetDate(),
                epic.createdAt(),
                epic.updatedAt(),
                epic.createdBy(),
                epic.updatedBy(),
                epic.deletedAt(),
                epic.metadata(),
                epic.version() != null ? epic.version() : 0L
        );
    }

    public Epic toDomain() {
        return new Epic(
                id,
                tenantId,
                projectId,
                name,
                summary,
                color,
                status,
                startDate,
                targetDate,
                createdAt,
                updatedAt,
                createdBy,
                updatedBy,
                deletedAt,
                metadata,
                version
        );
    }
}
