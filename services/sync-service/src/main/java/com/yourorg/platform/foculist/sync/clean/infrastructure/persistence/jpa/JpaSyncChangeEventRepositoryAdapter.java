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
    public SyncChangeEvent save(SyncChangeEvent syncChangeEvent) {
        return repository.save(SyncChangeEventJpaEntity.fromDomain(syncChangeEvent)).toDomain();
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
