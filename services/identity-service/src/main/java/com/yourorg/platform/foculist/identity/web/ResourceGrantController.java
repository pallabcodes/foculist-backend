package com.yourorg.platform.foculist.identity.web;

import com.yourorg.platform.foculist.identity.clean.application.service.ResourceGrantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.yourorg.platform.foculist.tenancy.TenantContext;
import com.yourorg.platform.foculist.identity.clean.domain.model.ResourceGrant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/v1/grants")
@RequiredArgsConstructor
public class ResourceGrantController {
    
    private final ResourceGrantService resourceGrantService;
    
    @GetMapping("/check")
    public ResponseEntity<Boolean> checkPermission(
            @RequestParam String userId,
            @RequestParam String resourceType,
            @RequestParam String resourceId,
            @RequestParam String requiredLevel) {
            
        boolean hasPerm = resourceGrantService.hasPermission(userId, resourceType, resourceId, requiredLevel);
        return ResponseEntity.ok(hasPerm);
    }
    
    @PreAuthorize("@RequiresRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ResourceGrant> grantPermission(@Valid @RequestBody GrantRequest request) {
        ResourceGrant grant = resourceGrantService.grantPermission(
                TenantContext.require(), 
                request.userId(), 
                request.resourceType(), 
                request.resourceId(), 
                request.permissionLevel()
        );
        return ResponseEntity.ok(grant);
    }
    
    @PreAuthorize("@RequiresRole('ADMIN')")
    @DeleteMapping
    public ResponseEntity<Void> revokePermission(
            @RequestParam String userId,
            @RequestParam String resourceType,
            @RequestParam String resourceId) {
            
        resourceGrantService.revokePermission(userId, resourceType, resourceId);
        return ResponseEntity.noContent().build();
    }
    
    public record GrantRequest(
            @NotBlank String userId,
            @NotBlank String resourceType,
            @NotBlank String resourceId,
            @NotBlank String permissionLevel
    ) {}
}
