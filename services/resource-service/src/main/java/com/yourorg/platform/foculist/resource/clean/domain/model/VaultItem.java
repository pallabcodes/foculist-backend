package com.yourorg.platform.foculist.resource.clean.domain.model;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public record VaultItem(
        UUID id,
        String tenantId,
        String name,
        String classification,
        Instant createdAt,
        long version
) {
    public VaultItem {
        if (id == null) {
            throw new ResourceDomainException("Vault item id is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new ResourceDomainException("Vault item tenantId is required");
        }
        if (name == null || name.isBlank()) {
            throw new ResourceDomainException("Vault item name is required");
        }
        if (createdAt == null) {
            throw new ResourceDomainException("Vault item createdAt is required");
        }
        if (version < 0) {
            throw new ResourceDomainException("Vault item version cannot be negative");
        }

        tenantId = tenantId.trim();
        name = name.trim();
        classification = normalizeClassification(classification);
    }

    public static VaultItem create(String tenantId, String name, String classification, Instant now) {
        return new VaultItem(
                UUID.randomUUID(),
                tenantId,
                name,
                classification,
                now == null ? Instant.now() : now,
                0L
        );
    }

    private static String normalizeClassification(String rawClassification) {
        if (rawClassification == null || rawClassification.isBlank()) {
            return "INTERNAL";
        }
        String normalized = rawClassification.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        if (normalized.length() > 32) {
            throw new ResourceDomainException("Vault classification is too long");
        }
        return normalized;
    }

    public VaultItem update(String name, String classification) {
        return new VaultItem(id, tenantId, name, classification, createdAt, version);
    }
}
