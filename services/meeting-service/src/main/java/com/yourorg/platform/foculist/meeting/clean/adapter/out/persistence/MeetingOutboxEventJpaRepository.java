package com.yourorg.platform.foculist.meeting.clean.adapter.out.persistence;

import com.yourorg.platform.foculist.meeting.clean.domain.model.MeetingOutboxEventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface MeetingOutboxEventJpaRepository extends JpaRepository<MeetingOutboxEventJpaEntity, UUID> {
    List<MeetingOutboxEventJpaEntity> findByStatusOrderByOccurredAtAsc(MeetingOutboxEventStatus status, Pageable pageable);
    int deleteByStatusAndOccurredAtBefore(MeetingOutboxEventStatus status, Instant threshold);
}
