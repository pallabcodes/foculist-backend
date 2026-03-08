package com.yourorg.platform.foculist.project.application;

import java.time.Instant;
import java.util.List;

public record ProjectTemplateView(
        String id,
        String key,
        String title,
        String description,
        List<String> categories,
        Object body,
        Instant updatedAt
) {
}
