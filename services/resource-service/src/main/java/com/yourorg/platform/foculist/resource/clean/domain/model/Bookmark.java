package com.yourorg.platform.foculist.resource.clean.domain.model;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

public record Bookmark(
        UUID id,
        String tenantId,
        String title,
        String url,
        Instant createdAt,
        long version
) {
    public Bookmark {
        if (id == null) {
            throw new ResourceDomainException("Bookmark id is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new ResourceDomainException("Bookmark tenantId is required");
        }
        if (title == null || title.isBlank()) {
            throw new ResourceDomainException("Bookmark title is required");
        }
        if (url == null || url.isBlank()) {
            throw new ResourceDomainException("Bookmark url is required");
        }
        if (createdAt == null) {
            throw new ResourceDomainException("Bookmark createdAt is required");
        }
        if (version < 0) {
            throw new ResourceDomainException("Bookmark version cannot be negative");
        }

        tenantId = tenantId.trim();
        title = title.trim();
        url = normalizeUrl(url);
    }

    public static Bookmark create(String tenantId, String title, String url, Instant now) {
        return new Bookmark(
                UUID.randomUUID(),
                tenantId,
                title,
                url,
                now == null ? Instant.now() : now,
                0L
        );
    }

    private static String normalizeUrl(String rawUrl) {
        String value = rawUrl.trim();
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new ResourceDomainException("Bookmark url must start with http or https");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new ResourceDomainException("Bookmark url host is missing");
            }
            return uri.toString();
        } catch (IllegalArgumentException ex) {
            throw new ResourceDomainException("Bookmark url is invalid");
        }
    }

    public Bookmark update(String title, String url) {
        return new Bookmark(id, tenantId, title, url, createdAt, version);
    }
}
