package com.yourorg.platform.foculist.project.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.project.domain.model.Project;
import com.yourorg.platform.foculist.project.domain.model.ProjectPriority;
import com.yourorg.platform.foculist.project.domain.model.ProjectStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.Map;

@Entity
@Table(
        name = "project_item",
        indexes = {
                @Index(name = "idx_project_item_tenant", columnList = "tenant_id"),
                @Index(name = "idx_project_item_tenant_status", columnList = "tenant_id,status"),
                @Index(name = "idx_project_item_deleted_at", columnList = "deleted_at")
        }
)
@SQLDelete(sql = "UPDATE project_item SET deleted_at = NOW() WHERE id = ? AND version = ?")
@Where(clause = "deleted_at IS NULL")
public class ProjectJpaEntity {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 128)
    private String tenantId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ProjectStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 32)
    private ProjectPriority priority;

    @Column(name = "due_date")
    private LocalDate dueDate;

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

    protected ProjectJpaEntity() {
    }

    private ProjectJpaEntity(
            UUID id,
            String tenantId,
            String name,
            String description,
            ProjectStatus status,
            ProjectPriority priority,
            LocalDate dueDate,
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
        this.name = name;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.deletedAt = deletedAt;
        this.metadata = metadata;
        this.version = version;
    }

    public static ProjectJpaEntity fromDomain(Project project) {
        return new ProjectJpaEntity(
                project.id(),
                project.tenantId(),
                project.name(),
                project.description(),
                project.status(),
                project.priority(),
                project.dueDate(),
                project.createdAt(),
                project.updatedAt(),
                project.createdBy(),
                project.updatedBy(),
                project.deletedAt(),
                project.metadata(),
                project.version()
        );
    }

    public Project toDomain() {
        return new Project(
                id,
                tenantId,
                name,
                description,
                status,
                priority,
                dueDate,
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
