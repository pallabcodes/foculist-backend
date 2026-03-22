package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardColumnJpaRepository extends JpaRepository<BoardColumnJpaEntity, UUID> {
    Optional<BoardColumnJpaEntity> findByIdAndTenantId(UUID id, String tenantId);
    List<BoardColumnJpaEntity> findByBoardIdAndTenantIdOrderByOrderIndexAsc(UUID boardId, String tenantId);
}
