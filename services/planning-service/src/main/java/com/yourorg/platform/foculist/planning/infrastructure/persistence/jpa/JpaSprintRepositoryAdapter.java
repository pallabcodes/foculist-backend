package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.planning.domain.model.Sprint;
import com.yourorg.platform.foculist.planning.domain.port.SprintRepositoryPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class JpaSprintRepositoryAdapter implements SprintRepositoryPort {
    private final SprintJpaRepository sprintJpaRepository;

    public JpaSprintRepositoryAdapter(SprintJpaRepository sprintJpaRepository) {
        this.sprintJpaRepository = sprintJpaRepository;
    }

    @Override
    public List<Sprint> findByTenantId(String tenantId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startDate"));
        return sprintJpaRepository.findByTenantId(tenantId, pageable).stream()
                .map(SprintJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Sprint> findByIdAndTenantId(UUID sprintId, String tenantId) {
        return sprintJpaRepository.findByIdAndTenantId(sprintId, tenantId)
                .map(SprintJpaEntity::toDomain);
    }

    @Override
    public Sprint save(Sprint sprint) {
        return sprintJpaRepository.save(SprintJpaEntity.fromDomain(sprint)).toDomain();
    }

    @Override
    public void delete(Sprint sprint) {
        sprintJpaRepository.delete(SprintJpaEntity.fromDomain(sprint));
    }
}
