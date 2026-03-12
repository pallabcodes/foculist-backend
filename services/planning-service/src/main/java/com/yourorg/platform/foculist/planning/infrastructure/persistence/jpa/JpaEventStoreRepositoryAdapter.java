package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.planning.domain.model.EventStore;
import com.yourorg.platform.foculist.planning.domain.port.EventStoreRepositoryPort;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JpaEventStoreRepositoryAdapter implements EventStoreRepositoryPort {
    private final EventStoreJpaRepository repository;

    public JpaEventStoreRepositoryAdapter(EventStoreJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(EventStore event) {
        repository.save(EventStoreJpaEntity.fromDomain(event));
    }

    @Override
    public List<EventStore> findByAggregateId(String tenantId, UUID aggregateId, String aggregateType) {
        return repository.findByAggregateIdAndAggregateTypeAndTenantIdOrderByVersionAsc(aggregateId, aggregateType, tenantId).stream()
                .map(EventStoreJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<EventStore> findByAggregateIdAfterVersion(String tenantId, UUID aggregateId, String aggregateType, long version) {
        return repository.findByAggregateIdAndAggregateTypeAndTenantIdAndVersionGreaterThanOrderByVersionAsc(aggregateId, aggregateType, tenantId, version).stream()
                .map(EventStoreJpaEntity::toDomain)
                .toList();
    }
}
