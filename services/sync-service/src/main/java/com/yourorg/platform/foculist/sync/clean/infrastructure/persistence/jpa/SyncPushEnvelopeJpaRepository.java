package com.yourorg.platform.foculist.sync.clean.infrastructure.persistence.jpa;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncPushEnvelopeJpaRepository extends JpaRepository<SyncPushEnvelopeJpaEntity, UUID> {
}
