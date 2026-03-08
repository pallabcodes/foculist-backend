package com.yourorg.platform.foculist.planning.domain.port;

import com.yourorg.platform.foculist.planning.domain.model.EventStore;
import java.util.List;
import java.util.UUID;

public interface EventStoreRepositoryPort {
    void save(EventStore event);
    List<EventStore> findByAggregateId(UUID aggregateId, String aggregateType);
}
