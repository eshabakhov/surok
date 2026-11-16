/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.User;
import java.util.List;

/**
 * Visitor without an account, identified by the ANONYMOUS role.
 * For example, {@code new UrAnonymous().roles().contains(Roles.RoleEnum.ANONYMOUS)} is true.
 *
 * @since 0.0.1
 */
public final class UrAnonymous implements User {

    /** Immutable anonymous role assignment. */
    private final Roles assigned;

    /**
     * New anonymous visitor.
     */
    public UrAnonymous() {
        this(new RlsFake(List.of(new RlNamed(Roles.RoleEnum.ANONYMOUS))));
    }

    /**
     * New anonymous visitor with immutable roles.
     *
     * @param assigned Anonymous role assignment
     */
    private UrAnonymous(final Roles assigned) {
        this.assigned = assigned;
    }

    @Override
    public Roles roles() {
        return this.assigned;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) {
        return media.with("authenticated", false).with("admin", false);
    }
}
