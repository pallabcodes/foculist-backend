package com.yourorg.platform.foculist.resource.clean.domain.port;

import com.yourorg.platform.foculist.resource.clean.domain.model.WorklogEntry;

public interface WorklogEntryRepositoryPort {
    java.util.List<WorklogEntry> findByTenantId(String tenantId, int page, int size);

    java.util.Optional<WorklogEntry> findByIdAndTenantId(java.util.UUID id, String tenantId);

    WorklogEntry save(WorklogEntry worklogEntry);

    void delete(WorklogEntry worklogEntry);
}
