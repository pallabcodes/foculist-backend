package com.yourorg.platform.foculist.project.infrastructure.persistence.jpa;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "project_permission_scheme")
@Getter
@Setter
public class ProjectPermissionSchemeJpaEntity {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Type(JsonBinaryType.class)
    @Column(name = "actions_mapping", columnDefinition = "jsonb", nullable = false)
    private Map<String, List<String>> actionsMapping;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
