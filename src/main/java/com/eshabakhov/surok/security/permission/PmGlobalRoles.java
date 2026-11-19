/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.Schools;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.security.right.Right;
import com.eshabakhov.surok.user.Roles;
import java.util.Set;

/**
 * Permission granted by a global role listed in the specification.
 * For example, global administration applies even when the actor has no schools.
 *
 * @since 0.0.1
 */
final class PmGlobalRoles implements Permission {

    /** Roles accepted by this grant. */
    private final Set<Roles.RoleEnum> roles;

    /**
     * New role grant.
     *
     * @param roles Declared roles
     */
    PmGlobalRoles(final Set<Roles.RoleEnum> roles) {
        this.roles = roles;
    }

    @Override
    public Right right(final UrAccount actor, final School school) {
        return this.global(actor);
    }

    @Override
    public Right right(final UrAccount actor, final Schools schools) {
        return this.global(actor);
    }

    private Right global(final UrAccount actor) {
        return () -> this.roles.stream().anyMatch(actor.roles()::contains);
    }
}
