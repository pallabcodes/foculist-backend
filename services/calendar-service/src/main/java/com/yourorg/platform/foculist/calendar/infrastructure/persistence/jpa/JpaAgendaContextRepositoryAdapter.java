package com.yourorg.platform.foculist.calendar.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.calendar.domain.model.AgendaContext;
import com.yourorg.platform.foculist.calendar.domain.port.AgendaContextRepositoryPort;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaAgendaContextRepositoryAdapter implements AgendaContextRepositoryPort {
    private final AgendaContextJpaRepository agendaContextRepository;

    public JpaAgendaContextRepositoryAdapter(AgendaContextJpaRepository agendaContextRepository) {
        this.agendaContextRepository = agendaContextRepository;
    }

    @Override
    public Optional<AgendaContext> findByTenantIdAndMeetingId(String tenantId, String meetingId) {
        return agendaContextRepository.findByTenantIdAndMeetingId(tenantId, meetingId)
                .map(AgendaContextJpaEntity::toDomain);
    }

    @Override
    public Optional<AgendaContext> findByIdAndTenantId(java.util.UUID id, String tenantId) {
        return agendaContextRepository.findByIdAndTenantId(id, tenantId)
                .map(AgendaContextJpaEntity::toDomain);
    }

    @Override
    public AgendaContext save(AgendaContext agendaContext) {
        return agendaContextRepository.save(AgendaContextJpaEntity.fromDomain(agendaContext)).toDomain();
    }

    @Override
    public void delete(AgendaContext agendaContext) {
        agendaContextRepository.delete(AgendaContextJpaEntity.fromDomain(agendaContext));
    }
}
