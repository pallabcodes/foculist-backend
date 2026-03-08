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

@Entity
@Table(
        name = "planning_task",
        indexes = {
                @Index(name = "idx_planning_task_tenant", columnList = "tenant_id"),
                @Index(name = "idx_planning_task_tenant_sprint", columnList = "tenant_id,sprint_id"),
                @Index(name = "idx_planning_task_tenant_status", columnList = "tenant_id,status")
        }
)
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
                version
        );
    }
}
