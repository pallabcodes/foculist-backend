package com.yourorg.platform.foculist.identity.clean.application.service;

import com.yourorg.platform.foculist.identity.clean.domain.model.MembershipRole;
import com.yourorg.platform.foculist.identity.clean.domain.model.Organization;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final OrganizationService organizationService;
    private final InviteService inviteService;
    private final com.yourorg.platform.foculist.identity.clean.adapter.out.persistence.OutboxEventRepository outboxEventRepository;

    @Transactional
    public Organization onboard(String projectName, String projectKey, UUID ownerId, String experience, List<String> invites) {
        // 1. Create Organization with metadata
        Organization org = organizationService.createOrganization(
                projectName,
                projectKey,
                ownerId,
                Map.of("experience", experience)
        );

        // 2. Dispatch Invites
        if (invites != null && !invites.isEmpty()) {
            for (String email : invites) {
                if (StringUtils.hasText(email)) {
                    inviteService.createInvite(org.getId(), ownerId, email.trim(), MembershipRole.MEMBER);
                }
            }
        }

        // 3. Record Transactional Outbox Event for Project Creation
        com.yourorg.platform.foculist.identity.clean.domain.model.OutboxEvent event = new com.yourorg.platform.foculist.identity.clean.domain.model.OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setTenantId(org.getId().toString()); // Use Org ID as Tenant Partition
        event.setCategory("WORKSPACE");
        event.setEventType("WORKSPACE_CREATED");
        event.setPayload(Map.of(
                "projectName", projectName,
                "projectKey", projectKey,
                "experience", experience,
                "ownerId", ownerId.toString()
        ));
        event.setStatus("PENDING");
        outboxEventRepository.save(event);

        return org;
    }
}
