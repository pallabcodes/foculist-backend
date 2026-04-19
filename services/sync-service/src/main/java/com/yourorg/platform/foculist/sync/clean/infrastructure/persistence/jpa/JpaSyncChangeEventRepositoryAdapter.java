package com.yourorg.platform.foculist.sync.clean.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.sync.clean.domain.model.SyncChangeEvent;
import com.yourorg.platform.foculist.sync.clean.domain.port.SyncChangeEventRepositoryPort;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class JpaSyncChangeEventRepositoryAdapter implements SyncChangeEventRepositoryPort {
    private final SyncChangeEventJpaRepository repository;

    public JpaSyncChangeEventRepositoryAdapter(SyncChangeEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @SuppressWarnings("null")
    public SyncChangeEvent save(SyncChangeEvent syncChangeEvent) {
        SyncChangeEventJpaEntity entity = SyncChangeEventJpaEntity.fromDomain(syncChangeEvent);
        SyncChangeEventJpaEntity saved = repository.save(entity);
        return saved.toDomain();
    }

    @Override
    public List<SyncChangeEvent> findSince(String tenantId, Instant sinceExclusive, int limit) {
        return repository.findByTenantIdAndOccurredAtGreaterThanOrderByOccurredAtAsc(
                        tenantId,
                        sinceExclusive,
                        PageRequest.of(0, limit)
                ).stream()
                .map(SyncChangeEventJpaEntity::toDomain)
                .toList();
    }
}
