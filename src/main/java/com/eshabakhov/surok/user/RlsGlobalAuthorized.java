/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.User;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;

/**
 * Global role assignments protected by the actor's current administrator role.
 * For example, {@code new RlsGlobalAuthorized(roles, actor).grant(role)}
 * refuses an actor whose administrator role has been revoked.
 *
 * @since 0.0.1
 */
public final class RlsGlobalAuthorized implements Roles {

    /** Underlying roles. */
    private final Roles origin;

    /** Acting user. */
    private final User actor;

    /**
     * New protected roles.
     *
     * @param origin Underlying global roles
     * @param actor Acting user
     */
    public RlsGlobalAuthorized(final Roles origin, final User actor) {
        this.origin = origin;
        this.actor = actor;
    }

    @Override
    public boolean contains(final RoleEnum role) {
        if (!this.actor.isAdmin()) {
            throw new AccessDeniedException("Only an administrator can inspect global roles");
        }
        return this.origin.contains(role);
    }

    @Override
    public Role grant(final RoleEnum role) throws Exception {
        if (!this.actor.isAdmin()) {
            throw new AccessDeniedException("Only an administrator can assign global roles");
        }
        return this.origin.grant(role);
    }

    @Override
    public List<Role> list() {
        if (!this.actor.isAdmin()) {
            throw new AccessDeniedException("Only an administrator can list global roles");
        }
        return this.origin.list();
    }

    @Override
    public void revoke(final long identity) throws Exception {
        if (!this.actor.isAdmin()) {
            throw new AccessDeniedException("Only an administrator can revoke global roles");
        }
        this.origin.revoke(identity);
    }
}
