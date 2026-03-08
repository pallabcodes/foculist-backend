package com.yourorg.platform.foculist.identity.clean.domain.repository;

import com.yourorg.platform.foculist.identity.clean.domain.model.ResourceGrant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResourceGrantRepository extends JpaRepository<ResourceGrant, String> {
    Optional<ResourceGrant> findByUserIdAndResourceTypeAndResourceId(String userId, String resourceType, String resourceId);
    boolean existsByUserIdAndResourceTypeAndResourceId(String userId, String resourceType, String resourceId);
}
