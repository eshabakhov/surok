/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import java.util.Set;

/**
 * A role represented by an account in a hypothetical school.
 * For example, new RiRole("DIRECTOR") allows evaluating ownership without database accounts.
 * This identity is never installed in the application's security context.
 *
 * @since 0.0.1
 */
public final class RiRole implements RoleIdentity {

    /** Inspected role. */
    private final String role;

    /**
     * New representative identity.
     *
     * @param role Role name
     */
    public RiRole(final String role) {
        this.role = role;
    }

    @Override
    public String getName() {
        return this.role;
    }

    @Override
    public RoleIdentity info() {
        return this;
    }

    @Override
    public Set<Long> schools() {
        return Set.of(1L);
    }
}
