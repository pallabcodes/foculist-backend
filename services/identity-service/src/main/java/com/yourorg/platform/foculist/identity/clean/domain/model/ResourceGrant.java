package com.yourorg.platform.foculist.identity.clean.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@Entity
@Table(name = "resource_grants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FilterDef(name = "tenantFilter", parameters = {@ParamDef(name = "tenantId", type = String.class)})
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class ResourceGrant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "resource_type", nullable = false)
    private String resourceType; // e.g., "Task", "Sprint"
    
    @Column(name = "resource_id", nullable = false)
    private String resourceId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "permission_level", nullable = false)
    private ResourceGrantLevel permissionLevel;
}
