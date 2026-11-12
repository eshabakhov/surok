/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.security.right.RtAdmin;
import com.eshabakhov.surok.security.right.RtEither;
import com.eshabakhov.surok.security.right.RtSchoolDirector;
import com.eshabakhov.surok.security.right.RtSchoolMember;
import java.util.List;

/**
 * School role assignments that check current membership on each operation.
 * For example, a director can grant a school role and a viewer can list roles.
 *
 * @since 0.0.1
 */
public final class RlsAuthorized implements Roles {

    /** Underlying assigned roles. */
    private final Roles origin;

    /** School whose roles are assigned. */
    private final School school;

    /** Acting user. */
    private final User actor;

    /**
     * New protected school role assignments.
     *
     * @param origin Underlying roles
     * @param school School whose roles are assigned
     * @param actor Acting user
     */
    public RlsAuthorized(final Roles origin, final School school, final User actor) {
        this.origin = origin;
        this.school = school;
        this.actor = actor;
    }

    @Override
    public boolean contains(final RoleEnum role) {
        return new RtEither(
            new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
        ).permit(this.origin).contains(role);
    }

    @Override
    public Role grant(final RoleEnum role) throws Exception {
        return new RtEither(
            new RtAdmin(this.actor), new RtSchoolDirector(this.actor, this.school)
        ).permit(this.origin).grant(role);
    }

    @Override
    public List<Role> list() {
        return new RtEither(
            new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
        ).permit(this.origin).list();
    }

    @Override
    public void revoke(final long identity) throws Exception {
        new RtEither(
            new RtAdmin(this.actor), new RtSchoolDirector(this.actor, this.school)
        ).permit(this.origin).revoke(identity);
    }
}
