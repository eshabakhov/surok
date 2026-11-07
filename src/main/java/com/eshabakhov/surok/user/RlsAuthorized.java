/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.User;
import com.eshabakhov.surok.Users;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;

/**
 * School role assignments that check current membership on each operation.
 * For example, a director can grant a school role and a viewer can list roles.
 *
 * @since 0.0.1
 */
public final class RlsAuthorized implements Roles {

    /** Underlying assigned roles. */
    private final Roles origin;

    /** Underlying school users. */
    private final Users members;

    /** Acting user. */
    private final User actor;

    /**
     * New protected school role assignments.
     *
     * @param origin Underlying roles
     * @param members Underlying school users
     * @param actor Acting user
     */
    public RlsAuthorized(final Roles origin, final Users members, final User actor) {
        this.origin = origin;
        this.members = members;
        this.actor = actor;
    }

    @Override
    public boolean contains(final RoleEnum role) {
        if (!this.actor.isAdmin() && !this.members.contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view school roles");
        }
        return this.origin.contains(role);
    }

    @Override
    public Role grant(final RoleEnum role) throws Exception {
        if (!this.actor.isAdmin() && (!this.members.contains(this.actor)
            || !this.members.roles(this.actor).contains(RoleEnum.DIRECTOR))) {
            throw new AccessDeniedException("Only a school director can manage roles");
        }
        return this.origin.grant(role);
    }

    @Override
    public List<Role> list() {
        if (!this.actor.isAdmin() && !this.members.contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view school roles");
        }
        return this.origin.list();
    }

    @Override
    public void revoke(final long identity) throws Exception {
        if (!this.actor.isAdmin() && (!this.members.contains(this.actor)
            || !this.members.roles(this.actor).contains(RoleEnum.DIRECTOR))) {
            throw new AccessDeniedException("Only a school director can manage roles");
        }
        this.origin.revoke(identity);
    }
}
