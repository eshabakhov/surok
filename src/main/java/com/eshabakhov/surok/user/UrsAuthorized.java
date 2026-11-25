/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.Users;
import com.eshabakhov.surok.security.Operations;

/**
 * School users whose operations check the acting user's current membership.
 * For example, a director may add a member, while a student may only read.
 *
 * @since 0.0.1
 */
public final class UrsAuthorized implements Users {

    /** Shared operation declarations. */
    private final Operations operations;

    /** Underlying school users. */
    private final Users origin;

    /** School whose users are protected. */
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
    public UrsAuthorized(
        final Users origin, final School school, final UrAccount actor,
        final Operations operations
    ) {
        this.origin = origin;
        this.school = school;
        this.actor = actor;
        this.operations = operations;
    }

    @Override
    public boolean contains(final UrAccount user) {
        return this.operations.operation("SchoolUsers.list")
            .permission(this.actor, this.school)
            .permit(this.origin).contains(user);
    }

    @Override
    public UrAccount add(final UrAccount user) throws Exception {
        return this.operations.operation("SchoolUsers.add")
            .permission(this.actor, this.school)
            .permit(this.origin).add(user);
    }

    @Override
    public Roles roles(final UrAccount user) {
        return new RlsAuthorized(
            this.operations.operation("SchoolRoles.list")
                .permission(this.actor, this.school)
                .permit(this.origin).roles(user), this.school, this.actor, this.operations
        );
    }

    @Override
    public Iterable<UrAccount> iterate() {
        return this.operations.operation("SchoolUsers.list")
            .permission(this.actor, this.school)
            .permit(this.origin).iterate();
    }

    @Override
    public UrAccount register(
        final String username, final String password, final String email,
        final Boolean corporate
    ) throws Exception {
        return this.operations.operation("SchoolUsers.attach")
            .permission(this.actor, this.school)
            .permit(this.origin).register(username, password, email, corporate);
    }

    @Override
    public UrAccount identification(final String login) throws Exception {
        return this.operations.operation("SchoolUsers.list")
            .permission(this.actor, this.school)
            .permit(this.origin).identification(login);
    }

    @Override
    public UrAccount identification(final Long id) throws Exception {
        return this.operations.operation("SchoolUsers.list")
            .permission(this.actor, this.school)
            .permit(this.origin).identification(id);
    }

    @Override
    public UrAccount authentication(final String login) throws Exception {
        return this.operations.operation("SchoolUsers.list")
            .permission(this.actor, this.school)
            .permit(this.origin).authentication(login);
    }

    @Override
    public void remove(final Long id) throws Exception {
        this.operations.operation("SchoolUsers.remove")
            .permission(this.actor, this.school)
            .permit(this.origin).remove(id);
    }
}
