/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.Users;
import com.eshabakhov.surok.security.Operations;
import java.util.stream.StreamSupport;

/**
 * Global users available only to the current administrator.
 * For example, {@code new UrsGlobalAuthorized(users, actor, operations).remove(id)}
 * checks the actor's administrator role before removing an account.
 *
 * @since 0.0.1
 */
public final class UrsGlobalAuthorized implements Users {

    /** Shared operation declarations. */
    private final Operations operations;

    /** Underlying global users. */
    private final Users origin;

    /** Acting user. */
    private final UrAccount actor;

    /**
     * New protected object with shared operation declarations.
     *
     * @param origin Underlying origin
     * @param actor Acting user
     * @param operations Shared operation declarations
     */
    public UrsGlobalAuthorized(
        final Users origin, final UrAccount actor,
        final Operations operations
    ) {
        this.origin = origin;
        this.actor = actor;
        this.operations = operations;
    }

    @Override
    public boolean contains(final UrAccount user) {
        return this.operations.operation("Users.read")
            .permission(this.actor)
            .permit(this.origin).contains(user);
    }

    @Override
    public UrAccount add(final UrAccount user) throws Exception {
        return new UrAcGlobalAuthorized(
            this.operations.operation("Users.add")
                .permission(this.actor)
                .permit(this.origin).add(user), this.actor, this.operations
        );
    }

    @Override
    public Roles roles(final UrAccount user) {
        return new RlsGlobalAuthorized(
            this.operations.operation("UserRoles.list")
                .permission(this.actor)
                .permit(this.origin).roles(user), this.actor, this.operations
        );
    }

    @Override
    public Iterable<UrAccount> iterate() {
        return StreamSupport.stream(
            this.operations.operation("Users.read")
                .permission(this.actor)
                .permit(this.origin).iterate().spliterator(), false
        ).<UrAccount>map(
            user -> new UrAcGlobalAuthorized(
                user, this.actor, this.operations
            )
        ).toList();
    }

    @Override
    public UrAccount register(
        final String username, final String password, final String email, final Boolean corporate
    ) throws Exception {
        return new UrAcGlobalAuthorized(
            this.operations.operation("Users.register")
                .permission(this.actor)
                .permit(this.origin).register(
                username, password, email, corporate
            ), this.actor, this.operations
        );
    }

    @Override
    public UrAccount identification(final String login) throws Exception {
        return new UrAcGlobalAuthorized(
            this.operations.operation("Users.read")
                .permission(this.actor)
                .permit(this.origin).identification(login), this.actor, this.operations
        );
    }

    @Override
    public UrAccount identification(final Long id) throws Exception {
        return new UrAcGlobalAuthorized(
            this.operations.operation("Users.read")
                .permission(this.actor)
                .permit(this.origin).identification(id), this.actor, this.operations
        );
    }

    @Override
    public UrAccount authentication(final String login) throws Exception {
        return new UrAcGlobalAuthorized(
            this.operations.operation("Users.read")
                .permission(this.actor)
                .permit(this.origin).authentication(login), this.actor, this.operations
        );
    }

    @Override
    public void remove(final Long id) throws Exception {
        this.operations.operation("Users.remove")
            .permission(this.actor)
            .permit(this.origin).remove(id);
    }
}
