package com.yourorg.platform.foculist.planning.domain.port;

import com.yourorg.platform.foculist.planning.domain.model.Sprint;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SprintRepositoryPort {
    List<Sprint> findByTenantId(String tenantId, int page, int size);

    Optional<Sprint> findByIdAndTenantId(UUID sprintId, String tenantId);

    Sprint save(Sprint sprint);

    void delete(Sprint sprint);
}
