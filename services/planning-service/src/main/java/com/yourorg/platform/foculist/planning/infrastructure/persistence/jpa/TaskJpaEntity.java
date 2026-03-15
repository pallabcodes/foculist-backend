package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.planning.domain.model.Task;
import com.yourorg.platform.foculist.planning.domain.model.TaskPriority;
import com.yourorg.platform.foculist.planning.domain.model.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.Map;

@Entity
@Table(
        name = "planning_task",
        indexes = {
                @Index(name = "idx_planning_task_tenant", columnList = "tenant_id"),
                @Index(name = "idx_planning_task_tenant_sprint", columnList = "tenant_id,sprint_id"),
                @Index(name = "idx_planning_task_tenant_status", columnList = "tenant_id,status"),
                @Index(name = "idx_planning_task_deleted_at", columnList = "deleted_at")
        }
)
@SQLDelete(sql = "UPDATE planning_task SET deleted_at = NOW() WHERE id = ? AND version = ?")
@Where(clause = "deleted_at IS NULL")
public class TaskJpaEntity {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 128)
    private String tenantId;

    @Column(name = "sprint_id")
    private UUID sprintId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 32)
    private TaskPriority priority;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected TaskJpaEntity() {
    }

    TaskJpaEntity(
            UUID id,
            String tenantId,
            UUID sprintId,
            String title,
            String description,
            TaskStatus status,
            TaskPriority priority,
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
        this.sprintId = sprintId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.deletedAt = deletedAt;
        this.metadata = metadata;
        this.version = version;
    }

    public static TaskJpaEntity fromDomain(Task task) {
        return new TaskJpaEntity(
                task.id(),
                task.tenantId(),
                task.sprintId(),
                task.title(),
                task.description(),
                task.status(),
                task.priority(),
                task.createdAt(),
                task.updatedAt(),
                task.createdBy(),
                task.updatedBy(),
                task.deletedAt(),
                task.metadata(),
                task.version() != null ? task.version() : 0L
        );
    }

    public Task toDomain() {
        return new Task(
                id,
                tenantId,
                sprintId,
                title,
                description,
                status,
                priority,
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
