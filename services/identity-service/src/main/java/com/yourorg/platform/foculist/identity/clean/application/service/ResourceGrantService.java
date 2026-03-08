package com.yourorg.platform.foculist.identity.clean.application.service;

import com.yourorg.platform.foculist.identity.clean.domain.model.ResourceGrant;
import com.yourorg.platform.foculist.identity.clean.domain.model.ResourceGrantLevel;
import com.yourorg.platform.foculist.identity.clean.domain.repository.ResourceGrantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceGrantService {
    
    private final ResourceGrantRepository resourceGrantRepository;
    
    @Transactional(readOnly = true)
    public boolean hasPermission(String userId, String resourceType, String resourceId, String requiredLevelStr) {
        log.debug("Checking permission for user {} on {}/{} requires {}", userId, resourceType, resourceId, requiredLevelStr);
        try {
            ResourceGrantLevel requiredLevel = ResourceGrantLevel.valueOf(requiredLevelStr.toUpperCase());
            Optional<ResourceGrant> grantOpt = resourceGrantRepository.findByUserIdAndResourceTypeAndResourceId(userId, resourceType, resourceId);
            
            if (grantOpt.isEmpty()) {
                return false;
            }
            
            ResourceGrantLevel actualLevel = grantOpt.get().getPermissionLevel();
            log.debug("Found explicit grant: {}", actualLevel);
            
            // ADMIN > WRITE > READ
            if (requiredLevel == ResourceGrantLevel.READ) {
                return true; // Any explicit grant implies read
            } else if (requiredLevel == ResourceGrantLevel.WRITE) {
                return actualLevel == ResourceGrantLevel.WRITE || actualLevel == ResourceGrantLevel.ADMIN;
            } else if (requiredLevel == ResourceGrantLevel.ADMIN) {
                return actualLevel == ResourceGrantLevel.ADMIN;
            }
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid required permission level: {}", requiredLevelStr);
            return false;
        }
    }
    
    @Transactional
    public ResourceGrant grantPermission(String tenantId, String userId, String resourceType, String resourceId, String levelStr) {
        ResourceGrantLevel level = ResourceGrantLevel.valueOf(levelStr.toUpperCase());
        
        Optional<ResourceGrant> existing = resourceGrantRepository.findByUserIdAndResourceTypeAndResourceId(userId, resourceType, resourceId);
        if (existing.isPresent()) {
            ResourceGrant grant = existing.get();
            grant.setPermissionLevel(level);
            return resourceGrantRepository.save(grant);
        }
        
        ResourceGrant grant = ResourceGrant.builder()
                .tenantId(tenantId)
                .userId(userId)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .permissionLevel(level)
                .build();
                
        return resourceGrantRepository.save(grant);
    }
    
    @Transactional
    public void revokePermission(String userId, String resourceType, String resourceId) {
        resourceGrantRepository.findByUserIdAndResourceTypeAndResourceId(userId, resourceType, resourceId)
                .ifPresent(resourceGrantRepository::delete);
    }
}
