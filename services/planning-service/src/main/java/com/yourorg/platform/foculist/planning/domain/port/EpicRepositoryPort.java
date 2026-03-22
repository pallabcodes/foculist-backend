package com.yourorg.platform.foculist.planning.domain.port;

import com.yourorg.platform.foculist.planning.domain.model.Epic;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface EpicRepositoryPort {
    Epic save(Epic epic);
    Optional<Epic> findByIdAndTenantId(UUID id, String tenantId);
    List<Epic> findByTenantId(String tenantId, int page, int size);
    List<Epic> findByProjectIdAndTenantId(UUID projectId, String tenantId, int page, int size);
    void delete(Epic epic);
}
