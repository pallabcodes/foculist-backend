package com.yourorg.platform.foculist.project.domain.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProjectPermissionScheme {
    private final UUID id;
    private final String tenantId;
    private final String name;
    private final String description;
    private final Map<String, List<String>> actionsMapping; // Role -> List of Actions

    public boolean isActionAllowed(String role, String action) {
        List<String> allowedActions = actionsMapping.getOrDefault(role.toUpperCase(), Collections.emptyList());
        return allowedActions.contains(action.toLowerCase());
    }

    public static ProjectPermissionScheme create(
            String tenantId,
            String name,
            Map<String, List<String>> mapping
    ) {
        return ProjectPermissionScheme.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .name(name)
                .actionsMapping(mapping)
                .build();
    }
}
