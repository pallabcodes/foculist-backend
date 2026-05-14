package com.yourorg.platform.foculist.identity.clean.adapter.out.persistence;

import com.yourorg.platform.foculist.identity.clean.domain.model.Membership;
import com.yourorg.platform.foculist.identity.clean.domain.model.Organization;
import com.yourorg.platform.foculist.identity.clean.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, UUID> {
    Optional<Membership> findByUserIdAndTenantId(UUID userId, String tenantId);
    java.util.List<Membership> findAllByOrganizationIdAndTenantId(UUID organizationId, String tenantId);
    java.util.List<Membership> findAllByUserId(UUID userId);
    Optional<Membership> findByOrganizationAndUserAndTenantId(Organization organization, User user, String tenantId);
}
