package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.planning.domain.model.ProjectionCheckpoint;
import com.yourorg.platform.foculist.planning.domain.port.ProjectionCheckpointRepositoryPort;
import org.springframework.stereotype.Component;

@Component
public class JpaProjectionCheckpointRepositoryAdapter implements ProjectionCheckpointRepositoryPort {
    private final ProjectionCheckpointJpaRepository repository;

    public JpaProjectionCheckpointRepositoryAdapter(ProjectionCheckpointJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public ProjectionCheckpoint findOrCreate(String projectionName) {
        return repository.findById(projectionName)
                .map(ProjectionCheckpointJpaEntity::toDomain)
                .orElseGet(() -> save(ProjectionCheckpoint.initial(projectionName)));
    }

    @Override
    public ProjectionCheckpoint save(ProjectionCheckpoint checkpoint) {
        return repository.save(ProjectionCheckpointJpaEntity.fromDomain(checkpoint)).toDomain();
    }
}
