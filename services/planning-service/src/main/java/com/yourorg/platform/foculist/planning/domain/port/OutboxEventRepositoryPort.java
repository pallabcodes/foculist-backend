package com.yourorg.platform.foculist.planning.domain.port;

import com.yourorg.platform.foculist.planning.domain.model.OutboxEvent;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepositoryPort {
    void save(OutboxEvent event);

    List<OutboxEvent> claimPendingBatch(int batchSize, int maxAttempts);

    void markPublished(UUID eventId, Instant publishedAt);

    void markFailed(UUID eventId, String errorMessage);

    int resetStuckProcessingEvents();
}
