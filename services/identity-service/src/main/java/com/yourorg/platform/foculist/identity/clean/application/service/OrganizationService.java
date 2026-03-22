package com.yourorg.platform.foculist.identity.clean.application.service;

import com.yourorg.platform.foculist.identity.clean.adapter.out.persistence.MembershipRepository;
import com.yourorg.platform.foculist.identity.clean.adapter.out.persistence.OrganizationRepository;
import com.yourorg.platform.foculist.identity.clean.adapter.out.persistence.UserRepository;
import com.yourorg.platform.foculist.identity.clean.domain.model.Membership;
import com.yourorg.platform.foculist.identity.clean.domain.model.MembershipRole;
import com.yourorg.platform.foculist.identity.clean.domain.model.Organization;
import com.yourorg.platform.foculist.identity.clean.domain.model.User;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yourorg.platform.foculist.tenancy.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OrganizationService {
    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;

    @Transactional
    public Organization createOrganization(String name, String slug, UUID ownerId, java.util.Map<String, Object> metadata) {
        // Owner is retrieved from the context they are currently in (e.g. 'public' upon signup)
        String currentTenant = TenantContext.require();
        User owner = userRepository.findByIdAndTenantId(ownerId, currentTenant)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found in current context"));

        Organization org = new Organization();
        org.setTenantId(slug); // The slug determines the new tenant partition
        org.setName(name);
        org.setSlug(slug);
        org.setTier("SOLO");
        if (metadata != null) {
            org.setMetadata(metadata);
        }
        org = organizationRepository.save(org);
        String realTenantId = org.getId().toString();

        Membership membership = new Membership();
        membership.setTenantId(realTenantId);
        membership.setOrganization(org);
        membership.setUser(owner);
        membership.setRole(MembershipRole.OWNER);
        membershipRepository.save(membership);

        return org;
    }

    public List<Organization> getUserOrganizations(UUID userId) {
        // Safe to query without tenant constraints because userId comes from secure JWT claims
        return membershipRepository.findAllByUserId(userId).stream()
                .map(Membership::getOrganization)
                .toList();
    }

    @Transactional
    public void addMember(UUID orgId, String email, MembershipRole role) {
        String tenantId = TenantContext.require();
        // Prevent cross-tenant leakage: ensure the org belongs to the current tenant context
        Organization org = organizationRepository.findByIdAndTenantId(orgId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found in context"));
        
        // Find user globally to invite them into the tenant
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found globally"));

        // Check if member already exists
        if (membershipRepository.findByOrganizationAndUserAndTenantId(org, user, tenantId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a member");
        }

        Membership membership = new Membership();
        membership.setTenantId(tenantId);
        membership.setOrganization(org);
        membership.setUser(user);
        membership.setRole(role);
        membershipRepository.save(membership);
    }

    public List<Membership> getMembers(UUID orgId) {
        String tenantId = TenantContext.require();
        // Prevent enumerating another tenant's members
        Organization org = organizationRepository.findByIdAndTenantId(orgId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found in context"));

        return membershipRepository.findAllByOrganizationIdAndTenantId(org.getId(), tenantId);
    }
}
