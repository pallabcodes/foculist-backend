package com.yourorg.platform.foculist.project.application;

import java.time.Instant;
import java.util.List;

public record BrainstormDocumentView(
        String id,
        String projectId,
        String summary,
        List<String> tags,
        String generatedBy,
        Object content,
        Instant updatedAt
) {
}
