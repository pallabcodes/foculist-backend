package com.yourorg.platform.foculist.resource.clean.domain.port;

import com.yourorg.platform.foculist.resource.clean.domain.model.Bookmark;
import java.util.List;

public interface BookmarkRepositoryPort {
    List<Bookmark> findByTenantId(String tenantId, int page, int size);

    java.util.Optional<Bookmark> findByIdAndTenantId(java.util.UUID id, String tenantId);

    Bookmark save(Bookmark bookmark);

    void delete(Bookmark bookmark);
}
