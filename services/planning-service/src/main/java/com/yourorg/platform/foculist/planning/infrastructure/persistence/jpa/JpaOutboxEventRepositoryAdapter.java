package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.planning.domain.model.OutboxEvent;
import com.yourorg.platform.foculist.planning.domain.model.OutboxEventStatus;
import com.yourorg.platform.foculist.planning.domain.model.PlanningDomainException;
import com.yourorg.platform.foculist.planning.domain.port.OutboxEventRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaOutboxEventRepositoryAdapter implements OutboxEventRepositoryPort {
    private final PlanningOutboxEventJpaRepository outboxRepository;

    public JpaOutboxEventRepositoryAdapter(PlanningOutboxEventJpaRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Override
    public void save(OutboxEvent event) {
        outboxRepository.save(PlanningOutboxEventJpaEntity.fromDomain(event));
    }

    @Override
    @Transactional
    public List<OutboxEvent> claimPendingBatch(int batchSize, int maxAttempts) {
        List<PlanningOutboxEventJpaEntity> entities = outboxRepository.findForPublish(
                List.of(OutboxEventStatus.NEW, OutboxEventStatus.FAILED),
                maxAttempts,
                PageRequest.of(0, batchSize)
        );
        entities.forEach(PlanningOutboxEventJpaEntity::markProcessing);
        return entities.stream().map(PlanningOutboxEventJpaEntity::toDomain).toList();
    }

    @Override
    @Transactional
    public void markPublished(UUID eventId, Instant publishedAt) {
        PlanningOutboxEventJpaEntity entity = outboxRepository.findById(eventId)
                .orElseThrow(() -> new PlanningDomainException("Outbox event does not exist: " + eventId));
        entity.markPublished(publishedAt);
    }

    @Override
    @Transactional
    public void markFailed(UUID eventId, String errorMessage) {
        PlanningOutboxEventJpaEntity entity = outboxRepository.findById(eventId)
                .orElseThrow(() -> new PlanningDomainException("Outbox event does not exist: " + eventId));
        entity.markFailed(errorMessage);
    }

    @Override
    @Transactional
    public int resetStuckProcessingEvents() {
        return outboxRepository.resetProcessingEvents();
    }
}
