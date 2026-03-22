package com.yourorg.platform.foculist.identity.clean.application.service;

import com.yourorg.platform.foculist.identity.clean.adapter.out.persistence.InviteRepository;
import com.yourorg.platform.foculist.identity.clean.adapter.out.persistence.MembershipRepository;
import com.yourorg.platform.foculist.identity.clean.domain.model.Invite;
import com.yourorg.platform.foculist.identity.clean.domain.model.Membership;
import com.yourorg.platform.foculist.identity.clean.domain.model.MembershipRole;
import com.yourorg.platform.foculist.identity.clean.domain.model.User;
import com.yourorg.platform.foculist.identity.clean.adapter.out.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InviteService {

    private final InviteRepository inviteRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final com.yourorg.platform.foculist.identity.clean.adapter.out.persistence.OrganizationRepository organizationRepository;
    private final com.yourorg.platform.foculist.identity.clean.adapter.out.persistence.OutboxEventRepository outboxEventRepository;

    @Transactional
    public Invite createInvite(UUID orgId, UUID inviterId, String email, MembershipRole role) {
        // Check if already a member
        userRepository.findByEmail(email).ifPresent(user -> {
            if (membershipRepository.findByUserIdAndTenantId(user.getId(), orgId.toString()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a member of this organization");
            }
        });

        // Check for existing pending invite
        inviteRepository.findByEmailAndOrgId(email, orgId).ifPresent(existing -> {
            if ("PENDING".equals(existing.getStatus()) && existing.getExpiresAt().isAfter(LocalDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A pending invitation already exists for this email");
            }
        });

        Invite invite = Invite.builder()
                .id(UUID.randomUUID())
                .orgId(orgId)
                .email(email)
                .inviterId(inviterId)
                .token(UUID.randomUUID().toString())
                .role(role != null ? role : MembershipRole.MEMBER)
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7)) // 7 day expiry
                .build();

        Invite savedInvite = inviteRepository.save(invite);

        // Record Transactional Outbox Event for Email Dispatch
        com.yourorg.platform.foculist.identity.clean.domain.model.OutboxEvent event = new com.yourorg.platform.foculist.identity.clean.domain.model.OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setTenantId(orgId.toString());
        event.setCategory("INVITE");
        event.setEventType("INVITE_CREATED");
        event.setPayload(Map.of(
                "inviteId", savedInvite.getId().toString(),
                "email", email,
                "token", savedInvite.getToken(),
                "role", savedInvite.getRole().name()
        ));
        event.setStatus("PENDING");
        outboxEventRepository.save(event);

        return savedInvite;
    }

    @Transactional
    public void acceptInvite(String token, UUID userId) {
        Invite invite = inviteRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation not found"));

        if (!"PENDING".equals(invite.getStatus())) {
            throw new ResponseStatusException(HttpStatus.GONE, "Invitation has already been " + invite.getStatus().toLowerCase());
        }

        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            invite.setStatus("EXPIRED");
            inviteRepository.save(invite);
            throw new ResponseStatusException(HttpStatus.GONE, "Invitation has expired");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Verify email match (Safety check)
        if (!invite.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This invitation was sent to a different email address");
        }

        com.yourorg.platform.foculist.identity.clean.domain.model.Organization organization = organizationRepository.findById(invite.getOrgId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));

        // Add to Membership
        Membership membership = new Membership();
        membership.setUser(user);
        membership.setOrganization(organization);
        membership.setTenantId(invite.getOrgId().toString()); 
        membership.setRole(invite.getRole());
        membershipRepository.save(membership);

        invite.setStatus("ACCEPTED");
        inviteRepository.save(invite);
    }
}
