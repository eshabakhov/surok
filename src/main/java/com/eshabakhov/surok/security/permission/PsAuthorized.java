/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.security.Operations;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Permission matrix visible only to the current administrator.
 * For example, {@code new PsAuthorized(permissions, actor, operations).print(media)}
 * checks the actor's current global role before printing the matrix.
 *
 * @since 0.0.1
 */
public final class PsAuthorized implements Permissions {

    /** Shared operation declarations. */
    private final Operations operations;

    /** Underlying permissions. */
    private final Permissions origin;

    /** Acting user. */
    private final UrAccount actor;

    /**
     * New protected object with shared operation declarations.
     *
     * @param origin Underlying origin
     * @param actor Acting user
     * @param operations Shared operation declarations
     */
    public PsAuthorized(
        final Permissions origin, final UrAccount actor,
        final Operations operations
    ) {
        this.origin = origin;
        this.actor = actor;
        this.operations = operations;
    }

    @Override
    public boolean allows(final String path, final RequestMethod method) {
        return this.operations.operation("Permissions.read")
            .permission(this.actor)
            .permit(this.origin).allows(path, method);
    }

    @Override
    public Permissions selection(final UrAccount caller) {
        return new PsAuthorized(
            this.operations.operation("Permissions.read")
                .permission(this.actor)
                .permit(this.origin).selection(caller), this.actor, this.operations
        );
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.operations.operation("Permissions.read")
            .permission(this.actor)
            .permit(this.origin).print(media);
    }
}
