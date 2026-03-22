package com.yourorg.platform.foculist.identity.clean.infrastructure.security;

import com.yourorg.platform.foculist.identity.clean.adapter.out.persistence.MembershipRepository;
import com.yourorg.platform.foculist.identity.clean.domain.model.Membership;
import com.yourorg.platform.foculist.identity.clean.domain.model.MembershipRole;
import com.yourorg.platform.foculist.tenancy.TenantContext;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component("orgSecurity")
@RequiredArgsConstructor
public class OrgSecurity {

    private final MembershipRepository membershipRepository;

    public boolean isPlatformAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            return false;
        }
        String globalRole = jwt.getClaimAsString("role");
        return "PLATFORM_ADMIN".equals(globalRole);
    }

    public boolean isAdmin(UUID orgId) {
        return hasAnyRole(orgId, MembershipRole.OWNER, MembershipRole.ADMIN);
    }

    public boolean isOwner(UUID orgId) {
        return hasAnyRole(orgId, MembershipRole.OWNER);
    }

    public boolean isMember(UUID orgId) {
        return hasAnyRole(orgId, MembershipRole.OWNER, MembershipRole.ADMIN, MembershipRole.MEMBER,
                MembershipRole.GUEST);
    }

    private boolean hasAnyRole(UUID orgId, MembershipRole... allowedRoles) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            return false;
        }

        String userIdStr = jwt.getClaimAsString("userId");
        if (userIdStr == null)
            return false;

        UUID userId;
        try {
            userId = UUID.fromString(userIdStr);
        } catch (IllegalArgumentException e) {
            return false;
        }

        String tenantId = TenantContext.get();
        if (tenantId == null) {
            return false; // If not operating in a known tenant context, deny by default.
        }

        Optional<Membership> memOpt = membershipRepository.findByUserIdAndTenantId(userId, tenantId);
        if (memOpt.isEmpty()) {
            return false;
        }

        MembershipRole userRole = memOpt.get().getRole();
        for (MembershipRole role : allowedRoles) {
            if (userRole == role) {
                return true;
            }
        }
        return false;
    }
}
