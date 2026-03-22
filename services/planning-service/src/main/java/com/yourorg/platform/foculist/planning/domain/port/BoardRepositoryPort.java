package com.yourorg.platform.foculist.planning.domain.port;

import com.yourorg.platform.foculist.planning.domain.model.Board;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface BoardRepositoryPort {
    Board save(Board board);
    Optional<Board> findByIdAndTenantId(UUID id, String tenantId);
    List<Board> findByTenantId(String tenantId, int page, int size);
    List<Board> findByProjectIdAndTenantId(UUID projectId, String tenantId, int page, int size);
    void delete(Board board);
}
