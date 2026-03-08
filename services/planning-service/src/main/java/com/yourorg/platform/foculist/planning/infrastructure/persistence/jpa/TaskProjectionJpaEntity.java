package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.planning.domain.model.TaskPriority;
import com.yourorg.platform.foculist.planning.domain.model.TaskProjection;
import com.yourorg.platform.foculist.planning.domain.model.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "planning_task_projection",
        indexes = {
                @Index(name = "idx_planning_task_projection_tenant", columnList = "tenant_id"),
                @Index(name = "idx_planning_task_projection_tenant_created", columnList = "tenant_id,created_at")
        }
)
public class TaskProjectionJpaEntity {
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

    @Column(name = "version", nullable = false)
    private long version;

    protected TaskProjectionJpaEntity() {
    }

    TaskProjectionJpaEntity(
            UUID id,
            String tenantId,
            UUID sprintId,
            String title,
            String description,
            TaskStatus status,
            TaskPriority priority,
            Instant createdAt,
            Instant updatedAt,
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
        this.version = version;
    }

    public static TaskProjectionJpaEntity fromDomain(TaskProjection projection) {
        return new TaskProjectionJpaEntity(
                projection.id(),
                projection.tenantId(),
                projection.sprintId(),
                projection.title(),
                projection.description(),
                projection.status(),
                projection.priority(),
                projection.createdAt(),
                projection.updatedAt(),
                projection.version()
        );
    }

    public TaskProjection toDomain() {
        return new TaskProjection(id, tenantId, sprintId, title, description, status, priority, createdAt, updatedAt, version);
    }
}
