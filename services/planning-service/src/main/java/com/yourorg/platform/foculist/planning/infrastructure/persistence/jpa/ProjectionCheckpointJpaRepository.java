package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectionCheckpointJpaRepository extends JpaRepository<ProjectionCheckpointJpaEntity, String> {
}
