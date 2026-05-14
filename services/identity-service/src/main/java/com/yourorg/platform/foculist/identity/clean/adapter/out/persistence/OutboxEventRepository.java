package com.yourorg.platform.foculist.identity.clean.adapter.out.persistence;

import com.yourorg.platform.foculist.identity.clean.domain.model.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(String status);
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);
    int deleteByStatusAndCreatedAtBefore(String status, OffsetDateTime threshold);
}
