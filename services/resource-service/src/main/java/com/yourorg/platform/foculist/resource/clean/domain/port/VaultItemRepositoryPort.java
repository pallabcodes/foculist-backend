package com.yourorg.platform.foculist.resource.clean.domain.port;

import com.yourorg.platform.foculist.resource.clean.domain.model.VaultItem;

public interface VaultItemRepositoryPort {
    java.util.List<VaultItem> findByTenantId(String tenantId, int page, int size);

    java.util.Optional<VaultItem> findByIdAndTenantId(java.util.UUID id, String tenantId);

    VaultItem save(VaultItem vaultItem);

    void delete(VaultItem vaultItem);
}
