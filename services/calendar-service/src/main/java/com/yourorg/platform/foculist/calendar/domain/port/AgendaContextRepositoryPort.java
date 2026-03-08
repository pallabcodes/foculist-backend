package com.yourorg.platform.foculist.calendar.domain.port;

import com.yourorg.platform.foculist.calendar.domain.model.AgendaContext;
import java.util.Optional;

public interface AgendaContextRepositoryPort {
    Optional<AgendaContext> findByTenantIdAndMeetingId(String tenantId, String meetingId);

    Optional<AgendaContext> findByIdAndTenantId(java.util.UUID id, String tenantId);

    AgendaContext save(AgendaContext agendaContext);

    void delete(AgendaContext agendaContext);
}
