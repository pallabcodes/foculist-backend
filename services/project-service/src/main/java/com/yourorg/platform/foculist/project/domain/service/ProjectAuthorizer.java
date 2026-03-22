package com.yourorg.platform.foculist.project.domain.service;

import com.yourorg.platform.foculist.project.domain.model.ProjectPermissionScheme;
import com.yourorg.platform.foculist.project.domain.model.Task;
import com.yourorg.platform.foculist.project.domain.port.ProjectMemberRepositoryPort;
import com.yourorg.platform.foculist.project.domain.port.ProjectPermissionSchemeRepositoryPort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectAuthorizer {

    private final ProjectMemberRepositoryPort memberRepository;
    private final ProjectPermissionSchemeRepositoryPort schemeRepository;

    /**
     * Resolves if a user can perform an action on a project/item.
     * Hierarchy: 
     * 1. Global Bypass (handled via orgRole in JWT - see CustomPermissionEvaluator)
     * 2. Dynamic Roles (Reporter/Assignee) 
     * 3. Static Project Roles (Admin, Developer, Viewer) resolved via Permission Scheme
     */
    public boolean can(UUID userId, String orgRole, UUID projectId, Task task, String action, String tenantId, UUID schemeId) {
        // 1. Org Admin Bypass
        if ("ADMIN".equalsIgnoreCase(orgRole) || "OWNER".equalsIgnoreCase(orgRole)) {
            return true;
        }

        // Fetch Scheme with fallback to 'system' tenant for global defaults
        ProjectPermissionScheme scheme = schemeRepository.findByIdAndTenantId(schemeId, tenantId)
                .or(() -> {
                    log.debug("Scheme {} not found in tenant {}, falling back to 'system'", schemeId, tenantId);
                    return schemeRepository.findByIdAndTenantId(schemeId, "system");
                })
                .orElse(null);
        
        if (scheme == null) {
            log.warn("No permission scheme found for project {} (scheme: {}) in tenant {} or system", 
                    projectId, schemeId, tenantId);
            return false;
        }

        // 2. Dynamic Roles (Task-specific)
        if (task != null) {
            if (userId.equals(task.getReporterId()) && scheme.isActionAllowed("REPORTER", action)) {
                return true;
            }
            if (userId.equals(task.getAssigneeId()) && scheme.isActionAllowed("ASSIGNEE", action)) {
                return true;
            }
        }

        // 3. Static Project Role
        String projectRole = memberRepository.getRole(projectId, userId, tenantId).orElse("VIEWER");
        return scheme.isActionAllowed(projectRole, action);
    }
}
