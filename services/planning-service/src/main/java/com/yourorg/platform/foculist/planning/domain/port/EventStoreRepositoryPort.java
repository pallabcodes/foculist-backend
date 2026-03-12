package com.yourorg.platform.foculist.planning.domain.port;

import com.yourorg.platform.foculist.planning.domain.model.EventStore;
import java.util.List;
import java.util.UUID;

public interface EventStoreRepositoryPort {
    void save(EventStore event);
    List<EventStore> findByAggregateId(String tenantId, UUID aggregateId, String aggregateType);
    List<EventStore> findByAggregateIdAfterVersion(String tenantId, UUID aggregateId, String aggregateType, long version);
}
