package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskProjectionJpaRepository extends JpaRepository<TaskProjectionJpaEntity, UUID> {
    Page<TaskProjectionJpaEntity> findByTenantId(String tenantId, Pageable pageable);

    @Query("""
            select t from TaskProjectionJpaEntity t
            where t.tenantId = :tenantId
              and (t.createdAt < :cursorCreatedAt
                   or (t.createdAt = :cursorCreatedAt and t.id < :cursorId))
            """)
    Page<TaskProjectionJpaEntity> findByTenantIdBeforeCursor(
            @Param("tenantId") String tenantId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    Optional<TaskProjectionJpaEntity> findByIdAndTenantId(UUID id, String tenantId);

    void deleteByIdAndTenantId(UUID id, String tenantId);
}
