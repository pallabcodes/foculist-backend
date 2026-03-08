package com.yourorg.platform.foculist.sync.clean.domain.port;

import com.yourorg.platform.foculist.sync.clean.domain.model.SyncPushEnvelope;

public interface SyncPushEnvelopeRepositoryPort {
    SyncPushEnvelope save(SyncPushEnvelope syncPushEnvelope);
}
