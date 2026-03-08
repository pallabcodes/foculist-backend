package com.yourorg.platform.foculist.calendar.infrastructure.persistence.jpa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarEventJpaRepository extends JpaRepository<CalendarEventJpaEntity, UUID> {
    List<CalendarEventJpaEntity> findByTenantIdOrderByDateAscTimeAsc(String tenantId);
    java.util.Optional<CalendarEventJpaEntity> findByIdAndTenantId(UUID id, String tenantId);
}
