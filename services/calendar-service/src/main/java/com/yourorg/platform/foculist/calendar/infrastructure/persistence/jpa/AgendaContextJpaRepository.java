package com.yourorg.platform.foculist.calendar.infrastructure.persistence.jpa;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgendaContextJpaRepository extends JpaRepository<AgendaContextJpaEntity, UUID> {
    Optional<AgendaContextJpaEntity> findByTenantIdAndMeetingId(String tenantId, String meetingId);
    Optional<AgendaContextJpaEntity> findByIdAndTenantId(UUID id, String tenantId);
}
