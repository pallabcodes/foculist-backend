package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.planning.domain.model.Board;
import com.yourorg.platform.foculist.planning.domain.model.BoardType;
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
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "planning_board",
        indexes = {
                @Index(name = "idx_planning_board_tenant", columnList = "tenant_id"),
                @Index(name = "idx_planning_board_tenant_project", columnList = "tenant_id,project_id"),
                @Index(name = "idx_planning_board_deleted_at", columnList = "deleted_at")
        }
)
@SQLDelete(sql = "UPDATE planning_board SET deleted_at = NOW() WHERE id = ? AND version = ?")
@Where(clause = "deleted_at IS NULL")
public class BoardJpaEntity {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 128)
    private String tenantId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private BoardType type;

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

    protected BoardJpaEntity() {
    }

    private BoardJpaEntity(
            UUID id,
            String tenantId,
            UUID projectId,
            String name,
            BoardType type,
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
        this.type = type;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.deletedAt = deletedAt;
        this.metadata = metadata;
        this.version = version;
    }

    public static BoardJpaEntity fromDomain(Board board) {
        return new BoardJpaEntity(
                board.id(),
                board.tenantId(),
                board.projectId(),
                board.name(),
                board.type(),
                board.createdAt(),
                board.updatedAt(),
                board.createdBy(),
                board.updatedBy(),
                board.deletedAt(),
                board.metadata(),
                board.version() != null ? board.version() : 0L
        );
    }

    public Board toDomain() {
        return new Board(
                id,
                tenantId,
                projectId,
                name,
                type,
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
