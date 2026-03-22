package com.yourorg.platform.foculist.planning.infrastructure.persistence.jpa;

import com.yourorg.platform.foculist.planning.domain.model.Epic;
import com.yourorg.platform.foculist.planning.domain.port.EpicRepositoryPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class JpaEpicRepositoryAdapter implements EpicRepositoryPort {
    private final EpicJpaRepository epicJpaRepository;

    public JpaEpicRepositoryAdapter(EpicJpaRepository epicJpaRepository) {
        this.epicJpaRepository = epicJpaRepository;
    }

    @Override
    public List<Epic> findByTenantId(String tenantId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return epicJpaRepository.findByTenantId(tenantId, pageable).stream()
                .map(EpicJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Epic> findByProjectIdAndTenantId(UUID projectId, String tenantId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return epicJpaRepository.findByProjectIdAndTenantId(projectId, tenantId, pageable).stream()
                .map(EpicJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Epic> findByIdAndTenantId(UUID id, String tenantId) {
        return epicJpaRepository.findByIdAndTenantId(id, tenantId)
                .map(EpicJpaEntity::toDomain);
    }

    @Override
    public Epic save(Epic epic) {
        return epicJpaRepository.save(EpicJpaEntity.fromDomain(epic)).toDomain();
    }

    @Override
    public void delete(Epic epic) {
        epicJpaRepository.delete(EpicJpaEntity.fromDomain(epic));
    }
}
