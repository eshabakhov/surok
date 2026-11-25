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
 * Account user whose school navigation returns protected domain objects.
 * For example, a controller uses this decorator for its authenticated principal.
 * Constructing the decorator does not verify a password or establish a login session.
 *
 * @since 0.0.1
 */
public final class UrAcAuthorized implements UrAccount {

    /** Shared operation declarations. */
    private final Operations operations;

    /** Acting account user. */
    private final UrAccount origin;

    /**
     * New protected object with shared operation declarations.
     *
     * @param origin Underlying origin
     * @param operations Shared operation declarations
     */
    public UrAcAuthorized(
        final UrAccount origin, final Operations operations
    ) {
        this.origin = origin;
        this.operations = operations;
    }

    @Override
    public long uid() {
        return this.origin.uid();
    }

    @Override
    public Account account() {
        return this.origin.account();
    }

    @Override
    public Roles roles() {
        return this.origin.roles();
    }

    @Override
    public Schools schools() {
        return new SlsAuthorized(this.origin.schools(), this.origin, this.operations);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.origin.print(media);
    }
}
