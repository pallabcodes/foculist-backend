package com.yourorg.platform.foculist.project.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

@Repository
interface TaskJpaRepository extends JpaRepository<TaskJpaEntity, UUID> {
    Optional<TaskJpaEntity> findByIdAndTenantId(UUID id, String tenantId);
    List<TaskJpaEntity> findByProjectIdAndTenantId(UUID projectId, String tenantId, Pageable pageable);
}

@Repository
interface ProjectPermissionSchemeJpaRepository extends JpaRepository<ProjectPermissionSchemeJpaEntity, UUID> {
    Optional<ProjectPermissionSchemeJpaEntity> findByIdAndTenantId(UUID id, String tenantId);
    Optional<ProjectPermissionSchemeJpaEntity> findByTenantIdAndName(String tenantId, String name);
}

@Repository
interface ProjectMemberJpaRepository extends JpaRepository<ProjectMemberJpaEntity, UUID> {
    Optional<ProjectMemberJpaEntity> findByProjectIdAndUserIdAndTenantId(UUID projectId, UUID userId, String tenantId);
}
