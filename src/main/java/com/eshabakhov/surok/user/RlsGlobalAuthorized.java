/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.security.Operations;
import java.util.List;

/**
 * Global role assignments protected by the actor's current administrator role.
 * For example, {@code new RlsGlobalAuthorized(roles, actor, operations).grant(role)}
 * refuses an actor whose administrator role has been revoked.
 *
 * @since 0.0.1
 */
public final class RlsGlobalAuthorized implements Roles {

    /** Shared operation declarations. */
    private final Operations operations;

    /** Underlying roles. */
    private final Roles origin;

    /** Acting user. */
    private final UrAccount actor;

    /**
     * New protected object with shared operation declarations.
     *
     * @param origin Underlying origin
     * @param actor Acting user
     * @param operations Shared operation declarations
     */
    public RlsGlobalAuthorized(
        final Roles origin, final UrAccount actor,
        final Operations operations
    ) {
        this.origin = origin;
        this.actor = actor;
        this.operations = operations;
    }

    @Override
    public boolean contains(final RoleEnum role) {
        return this.operations.operation("UserRoles.list")
            .permission(this.actor)
            .permit(this.origin).contains(role);
    }

    @Override
    public Role grant(final RoleEnum role) throws Exception {
        return this.operations.operation("UserRoles.grant")
            .permission(this.actor)
            .permit(this.origin).grant(role);
    }

    @Override
    public List<Role> list() {
        return this.operations.operation("UserRoles.list")
            .permission(this.actor)
            .permit(this.origin).list();
    }

    @Override
    public void revoke(final long identity) throws Exception {
        this.operations.operation("UserRoles.revoke")
            .permission(this.actor)
            .permit(this.origin).revoke(identity);
    }
}
