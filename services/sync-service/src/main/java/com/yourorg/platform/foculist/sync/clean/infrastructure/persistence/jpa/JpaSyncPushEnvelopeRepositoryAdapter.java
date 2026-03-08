package com.yourorg.platform.foculist.sync.clean.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.sync.clean.domain.model.SyncPushEnvelope;
import com.yourorg.platform.foculist.sync.clean.domain.port.SyncPushEnvelopeRepositoryPort;
import org.springframework.stereotype.Component;

@Component
public class JpaSyncPushEnvelopeRepositoryAdapter implements SyncPushEnvelopeRepositoryPort {
    private final SyncPushEnvelopeJpaRepository repository;

    public JpaSyncPushEnvelopeRepositoryAdapter(SyncPushEnvelopeJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public SyncPushEnvelope save(SyncPushEnvelope syncPushEnvelope) {
        return repository.save(SyncPushEnvelopeJpaEntity.fromDomain(syncPushEnvelope)).toDomain();
    }
}
