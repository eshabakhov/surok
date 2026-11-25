/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.security.Operations;
import java.util.List;

/**
 * School role assignments that check current membership on each operation.
 * For example, a director can grant a school role and a student can list roles.
 *
 * @since 0.0.1
 */
public final class RlsAuthorized implements Roles {

    /** Shared operation declarations. */
    private final Operations operations;

    /** Underlying assigned roles. */
    private final Roles origin;

    /** School whose roles are assigned. */
    private final School school;

    /** Acting user. */
    private final UrAccount actor;

    /**
     * New protected object with shared operation declarations.
     *
     * @param origin Underlying origin
     * @param school Underlying school
     * @param actor Acting user
     * @param operations Shared operation declarations
     */
    public RlsAuthorized(
        final Roles origin, final School school, final UrAccount actor,
        final Operations operations
    ) {
        this.origin = origin;
        this.school = school;
        this.actor = actor;
        this.operations = operations;
    }

    @Override
    public boolean contains(final RoleEnum role) {
        return this.operations.operation("SchoolRoles.list")
            .permission(this.actor, this.school)
            .permit(this.origin).contains(role);
    }

    @Override
    public Role grant(final RoleEnum role) throws Exception {
        return this.operations.operation("SchoolRoles.grant")
            .permission(this.actor, this.school)
            .permit(this.origin).grant(role);
    }

    @Override
    public List<Role> list() {
        return this.operations.operation("SchoolRoles.list")
            .permission(this.actor, this.school)
            .permit(this.origin).list();
    }

    @Override
    public void revoke(final long identity) throws Exception {
        this.operations.operation("SchoolRoles.revoke")
            .permission(this.actor, this.school)
            .permit(this.origin).revoke(identity);
    }
}
