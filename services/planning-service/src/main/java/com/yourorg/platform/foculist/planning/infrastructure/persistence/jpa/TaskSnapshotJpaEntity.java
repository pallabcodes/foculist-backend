package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.planning.domain.model.TaskPriority;
import com.yourorg.platform.foculist.planning.domain.model.TaskSnapshot;
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

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.Map;

@Entity
@Table(
        name = "planning_task_snapshot",
        indexes = {
                @Index(name = "idx_planning_task_snapshot_task_version", columnList = "tenant_id,task_id,version")
        }
)
public class TaskSnapshotJpaEntity {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 128)
    private String tenantId;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "sprint_id")
    private UUID sprintId;

    @Column(name = "epic_id")
    private UUID epicId;

    @Column(name = "board_column_id")
    private UUID boardColumnId;

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

    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "snapshotted_at", nullable = false)
    private Instant snapshottedAt;

    protected TaskSnapshotJpaEntity() {
    }

    TaskSnapshotJpaEntity(
            UUID id,
            String tenantId,
            UUID taskId,
            UUID sprintId,
            UUID epicId,
            UUID boardColumnId,
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
            long version,
            Instant snapshottedAt
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.taskId = taskId;
        this.sprintId = sprintId;
        this.epicId = epicId;
        this.boardColumnId = boardColumnId;
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
        this.snapshottedAt = snapshottedAt;
    }

    public static TaskSnapshotJpaEntity fromDomain(TaskSnapshot snapshot) {
        return new TaskSnapshotJpaEntity(
                snapshot.id(),
                snapshot.tenantId(),
                snapshot.taskId(),
                snapshot.sprintId(),
                snapshot.epicId(),
                snapshot.boardColumnId(),
                snapshot.title(),
                snapshot.description(),
                snapshot.status(),
                snapshot.priority(),
                snapshot.createdAt(),
                snapshot.updatedAt(),
                snapshot.createdBy(),
                snapshot.updatedBy(),
                snapshot.deletedAt(),
                snapshot.metadata(),
                snapshot.version(),
                snapshot.snapshottedAt()
        );
    }

    public TaskSnapshot toDomain() {
        return new TaskSnapshot(
                id,
                tenantId,
                taskId,
                sprintId,
                epicId,
                boardColumnId,
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
                version,
                snapshottedAt
        );
    }
}
