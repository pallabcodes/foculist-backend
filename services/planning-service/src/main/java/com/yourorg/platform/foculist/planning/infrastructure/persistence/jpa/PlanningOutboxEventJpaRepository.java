package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.planning.domain.model.OutboxEventStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlanningOutboxEventJpaRepository extends JpaRepository<PlanningOutboxEventJpaEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select e
            from PlanningOutboxEventJpaEntity e
            where e.status in :statuses
              and e.attempts < :maxAttempts
            order by e.occurredAt asc
            """)
    List<PlanningOutboxEventJpaEntity> findForPublish(
            @Param("statuses") Collection<OutboxEventStatus> statuses,
            @Param("maxAttempts") int maxAttempts,
            Pageable pageable
    );

    @org.springframework.data.jpa.repository.Modifying
    @Query("update PlanningOutboxEventJpaEntity e set e.status = 'NEW' where e.status = 'PROCESSING'")
    int resetProcessingEvents();

    @Query("select e from PlanningOutboxEventJpaEntity e where e.occurredAt > :after and e.tenantId = :tenantId order by e.occurredAt asc")
    List<PlanningOutboxEventJpaEntity> findBatchAfter(@Param("after") Instant after, @Param("tenantId") String tenantId, Pageable pageable);
}
