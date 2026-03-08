package com.yourorg.platform.foculist.tenancy.security;

import com.yourorg.platform.foculist.tenancy.domain.MembershipRole;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that enforces {@link RequiresRole} annotations.
 * <p>
 * Extracts the {@code orgRole} claim from the authenticated JWT and
 * compares it against the minimum role required by the annotation.
 * Throws {@link AccessDeniedException} if the caller's role rank is
 * insufficient.
 */
@Aspect
@Component
public class RoleSecurityAspect {

    @Before("@annotation(requiresRole)")
    public void enforceRole(JoinPoint joinPoint, RequiresRole requiresRole) {
        MembershipRole required = requiresRole.value();
        MembershipRole actual = extractOrgRole();

        if (!actual.isAtLeast(required)) {
            throw new AccessDeniedException(
                    "Insufficient role: required %s but caller has %s".formatted(required, actual)
            );
        }
    }

    private MembershipRole extractOrgRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new AccessDeniedException("No authentication found in SecurityContext");
        }

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String orgRoleClaim = jwt.getClaimAsString("orgRole");
            if (orgRoleClaim != null) {
                try {
                    return MembershipRole.valueOf(orgRoleClaim);
                } catch (IllegalArgumentException e) {
                    throw new AccessDeniedException("Unknown orgRole claim: " + orgRoleClaim);
                }
            }
        }

        // Fallback: check for X-User-Role header (propagated by gateway)
        // This is handled by downstream services that receive the header
        return MembershipRole.GUEST;
    }
}
