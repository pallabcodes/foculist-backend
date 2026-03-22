package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.planning.domain.model.BoardColumn;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(
        name = "planning_board_column",
        indexes = {
                @Index(name = "idx_planning_board_column_board", columnList = "board_id"),
                @Index(name = "idx_planning_board_column_deleted_at", columnList = "deleted_at")
        }
)
@SQLDelete(sql = "UPDATE planning_board_column SET deleted_at = NOW() WHERE id = ? AND version = ?")
@Where(clause = "deleted_at IS NULL")
public class BoardColumnJpaEntity {
    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 128)
    private String tenantId;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "status_mapping", length = 128)
    private String statusMapping;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

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

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected BoardColumnJpaEntity() {
    }

    private BoardColumnJpaEntity(
            UUID id,
            String tenantId,
            UUID boardId,
            String name,
            String statusMapping,
            int orderIndex,
            Instant createdAt,
            Instant updatedAt,
            String createdBy,
            String updatedBy,
            Instant deletedAt,
            long version
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.boardId = boardId;
        this.name = name;
        this.statusMapping = statusMapping;
        this.orderIndex = orderIndex;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.deletedAt = deletedAt;
        this.version = version;
    }

    public static BoardColumnJpaEntity fromDomain(BoardColumn column) {
        return new BoardColumnJpaEntity(
                column.id(),
                column.tenantId(),
                column.boardId(),
                column.name(),
                column.statusMapping(),
                column.orderIndex(),
                column.createdAt(),
                column.updatedAt(),
                column.createdBy(),
                column.updatedBy(),
                column.deletedAt(),
                column.version() != null ? column.version() : 0L
        );
    }

    public BoardColumn toDomain() {
        return new BoardColumn(
                id,
                tenantId,
                boardId,
                name,
                statusMapping,
                orderIndex,
                createdAt,
                updatedAt,
                createdBy,
                updatedBy,
                deletedAt,
                version
        );
    }
}
