package com.yourorg.platform.foculist.planning.domain.port;

import com.yourorg.platform.foculist.planning.domain.model.BoardColumn;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface BoardColumnRepositoryPort {
    BoardColumn save(BoardColumn column);
    Optional<BoardColumn> findByIdAndTenantId(UUID id, String tenantId);
    List<BoardColumn> findByBoardIdAndTenantId(UUID boardId, String tenantId);
    void delete(BoardColumn column);
}
