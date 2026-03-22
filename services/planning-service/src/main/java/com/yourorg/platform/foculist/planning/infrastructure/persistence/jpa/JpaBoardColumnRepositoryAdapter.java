package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.planning.domain.model.BoardColumn;
import com.yourorg.platform.foculist.planning.domain.port.BoardColumnRepositoryPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JpaBoardColumnRepositoryAdapter implements BoardColumnRepositoryPort {
    private final BoardColumnJpaRepository columnJpaRepository;

    public JpaBoardColumnRepositoryAdapter(BoardColumnJpaRepository columnJpaRepository) {
        this.columnJpaRepository = columnJpaRepository;
    }

    @Override
    public List<BoardColumn> findByBoardIdAndTenantId(UUID boardId, String tenantId) {
        return columnJpaRepository.findByBoardIdAndTenantIdOrderByOrderIndexAsc(boardId, tenantId).stream()
                .map(BoardColumnJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<BoardColumn> findByIdAndTenantId(UUID id, String tenantId) {
        return columnJpaRepository.findByIdAndTenantId(id, tenantId)
                .map(BoardColumnJpaEntity::toDomain);
    }

    @Override
    public BoardColumn save(BoardColumn column) {
        return columnJpaRepository.save(BoardColumnJpaEntity.fromDomain(column)).toDomain();
    }

    @Override
    public void delete(BoardColumn column) {
        columnJpaRepository.delete(BoardColumnJpaEntity.fromDomain(column));
    }
}
