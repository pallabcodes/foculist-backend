package com.yourorg.platform.foculist.tenancy.security;

import com.yourorg.platform.foculist.tenancy.domain.MembershipRole;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declarative role guard for controller methods.
 * <p>
 * The annotated endpoint requires the caller's JWT {@code orgRole} claim
 * to be at least as privileged as the specified {@link MembershipRole}.
 * <p>
 * Role hierarchy (highest to lowest): {@code OWNER > ADMIN > MEMBER > GUEST}.
 *
 * <pre>
 * {@code
 * @RequiresRole(MembershipRole.ADMIN)
 * @DeleteMapping("/projects/{id}")
 * public void deleteProject(@PathVariable UUID id) { ... }
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresRole {
    MembershipRole value();
}
