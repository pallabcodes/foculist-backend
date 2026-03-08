package com.yourorg.platform.foculist.project.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.project.domain.model.ProjectDefaultView;
import com.yourorg.platform.foculist.project.domain.model.ProjectSettings;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "project_settings",
        indexes = {
                @Index(name = "idx_project_settings_tenant", columnList = "tenant_id")
        }
)
public class ProjectSettingsJpaEntity {
    @Id
    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "tenant_id", nullable = false, length = 128)
    private String tenantId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "project_workflow_status",
            joinColumns = @JoinColumn(name = "project_id")
    )
    @OrderColumn(name = "position")
    @Column(name = "status", nullable = false, length = 64)
    private List<String> workflowStatuses = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "default_view", nullable = false, length = 32)
    private ProjectDefaultView defaultView;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ProjectSettingsJpaEntity() {
    }

    private ProjectSettingsJpaEntity(
            UUID projectId,
            String tenantId,
            List<String> workflowStatuses,
            ProjectDefaultView defaultView,
            Instant updatedAt,
            long version
    ) {
        this.projectId = projectId;
        this.tenantId = tenantId;
        this.workflowStatuses = workflowStatuses;
        this.defaultView = defaultView;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static ProjectSettingsJpaEntity fromDomain(ProjectSettings settings) {
        return new ProjectSettingsJpaEntity(
                settings.projectId(),
                settings.tenantId(),
                new ArrayList<>(settings.workflowStatuses()),
                settings.defaultView(),
                settings.updatedAt(),
                settings.version()
        );
    }

    public ProjectSettings toDomain() {
        return new ProjectSettings(
                projectId,
                tenantId,
                workflowStatuses,
                defaultView,
                updatedAt,
                version
        );
    }
}
