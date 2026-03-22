package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.planning.domain.model.Board;
import com.yourorg.platform.foculist.planning.domain.port.BoardRepositoryPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class JpaBoardRepositoryAdapter implements BoardRepositoryPort {
    private final BoardJpaRepository boardJpaRepository;

    public JpaBoardRepositoryAdapter(BoardJpaRepository boardJpaRepository) {
        this.boardJpaRepository = boardJpaRepository;
    }

    @Override
    public List<Board> findByTenantId(String tenantId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return boardJpaRepository.findByTenantId(tenantId, pageable).stream()
                .map(BoardJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Board> findByProjectIdAndTenantId(UUID projectId, String tenantId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return boardJpaRepository.findByProjectIdAndTenantId(projectId, tenantId, pageable).stream()
                .map(BoardJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Board> findByIdAndTenantId(UUID id, String tenantId) {
        return boardJpaRepository.findByIdAndTenantId(id, tenantId)
                .map(BoardJpaEntity::toDomain);
    }

    @Override
    public Board save(Board board) {
        return boardJpaRepository.save(BoardJpaEntity.fromDomain(board)).toDomain();
    }

    @Override
    public void delete(Board board) {
        boardJpaRepository.delete(BoardJpaEntity.fromDomain(board));
    }
}
