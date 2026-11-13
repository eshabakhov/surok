/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import java.util.Collection;
import java.util.List;

/**
 * Immutable role assignments for account inspection.
 * For example, {@code new RlsFixed(List.of())} exposes no roles to an anonymous visitor.
 *
 * @since 0.0.1
 */
public final class RlsFixed implements Roles {

    /** Represented roles. */
    private final List<Role> items;

    /**
     * New fixed roles.
     *
     * @param items Represented assignments
     */
    public RlsFixed(final Collection<Role> items) {
        this(List.copyOf(items));
    }

    /**
     * New roles backed by an immutable list.
     *
     * @param items Immutable assignments
     */
    private RlsFixed(final List<Role> items) {
        this.items = items;
    }

    @Override
    public boolean contains(final RoleEnum role) {
        return this.items.stream().anyMatch(assigned -> role.name().equals(assigned.name()));
    }

    @Override
    public Role grant(final RoleEnum role) {
        throw new UnsupportedOperationException(
            String.format("Read-only roles cannot grant %s", role)
        );
    }

    @Override
    public List<Role> list() {
        return this.items;
    }

    @Override
    public void revoke(final long identity) {
        throw new UnsupportedOperationException(
            String.format("Read-only roles cannot revoke assignment %d", identity)
        );
    }
}
