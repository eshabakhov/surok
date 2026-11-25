/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Schools;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.school.SlsAuthorized;
import com.eshabakhov.surok.security.Operations;

/**
 * Global user whose account data is available to the current administrator.
 * For example, {@code new UrAcGlobalAuthorized(user, actor, operations).roles()}
 * returns roles that recheck the actor before every operation.
 *
 * @since 0.0.1
 */
public final class UrAcGlobalAuthorized implements UrAccount {

    /** Shared operation declarations. */
    private final Operations operations;

    /** Underlying user. */
    private final UrAccount origin;

    /** Acting user. */
    private final UrAccount actor;

    /**
     * New protected object with shared operation declarations.
     *
     * @param origin Underlying origin
     * @param actor Acting user
     * @param operations Shared operation declarations
     */
    public UrAcGlobalAuthorized(
        final UrAccount origin, final UrAccount actor,
        final Operations operations
    ) {
        this.origin = origin;
        this.actor = actor;
        this.operations = operations;
    }

    @Override
    public long uid() {
        return this.origin.uid();
    }

    @Override
    public Account account() {
        return this.operations.operation("Users.read")
            .permission(this.actor)
            .permit(this.origin).account();
    }

    @Override
    public Roles roles() {
        return new RlsGlobalAuthorized(
            this.operations.operation("UserRoles.list")
                .permission(this.actor)
                .permit(this.origin).roles(), this.actor, this.operations
        );
    }

    @Override
    public Schools schools() {
        return new SlsAuthorized(
            this.operations.operation("Users.read")
                .permission(this.actor)
                .permit(this.origin).schools(), this.actor, this.operations
        );
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.operations.operation("Users.read")
            .permission(this.actor)
            .permit(this.origin).print(media);
    }
}
