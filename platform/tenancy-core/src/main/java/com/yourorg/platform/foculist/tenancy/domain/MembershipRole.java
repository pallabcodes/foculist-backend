package com.yourorg.platform.foculist.tenancy.domain;

/**
 * Organization membership roles, ordered by privilege level (highest first).
 * <p>
 * Shared across all services via tenancy-core. The identity-service's
 * {@code identity.clean.domain.model.MembershipRole} mirrors this enum
 * for JPA persistence.
 */
public enum MembershipRole {
    OWNER(4),
    ADMIN(3),
    MEMBER(2),
    GUEST(1);

    private final int rank;

    MembershipRole(int rank) {
        this.rank = rank;
    }

    /** Returns the privilege rank (higher = more privileged). */
    public int rank() {
        return rank;
    }

    /**
     * Returns true if this role is at least as privileged as the required role.
     * E.g. {@code ADMIN.isAtLeast(MEMBER)} returns true.
     */
    public boolean isAtLeast(MembershipRole required) {
        return this.rank >= required.rank;
    }
}
