package com.yourorg.platform.foculist.planning.domain.port;

import com.yourorg.platform.foculist.planning.domain.model.ProjectionCheckpoint;

public interface ProjectionCheckpointRepositoryPort {
    ProjectionCheckpoint findOrCreate(String projectionName);
    ProjectionCheckpoint save(ProjectionCheckpoint checkpoint);
}
