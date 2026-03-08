package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.planning.domain.model.OutboxEventStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskSnapshotJobJpaRepository extends JpaRepository<TaskSnapshotJobJpaEntity, java.util.UUID> {
    boolean existsByTenantIdAndTaskIdAndTargetVersion(String tenantId, java.util.UUID taskId, long targetVersion);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select j
            from TaskSnapshotJobJpaEntity j
            where j.status in :statuses
              and j.attempts < :maxAttempts
            order by j.createdAt asc
            """)
    List<TaskSnapshotJobJpaEntity> findForProcessing(
            @Param("statuses") List<OutboxEventStatus> statuses,
            @Param("maxAttempts") int maxAttempts,
            Pageable pageable
    );
}
