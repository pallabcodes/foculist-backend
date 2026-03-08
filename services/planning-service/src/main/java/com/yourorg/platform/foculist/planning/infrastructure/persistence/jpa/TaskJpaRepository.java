package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskJpaRepository extends JpaRepository<TaskJpaEntity, UUID> {
    List<TaskJpaEntity> findByTenantId(String tenantId);
    Page<TaskJpaEntity> findByTenantId(String tenantId, Pageable pageable);

    @Query("""
            select t from TaskJpaEntity t
            where t.tenantId = :tenantId
              and (t.createdAt < :cursorCreatedAt
                   or (t.createdAt = :cursorCreatedAt and t.id < :cursorId))
            """)
    Page<TaskJpaEntity> findByTenantIdBeforeCursor(
            @Param("tenantId") String tenantId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );
    Optional<TaskJpaEntity> findByIdAndTenantId(UUID id, String tenantId);
}
