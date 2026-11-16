/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Schools;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.school.SlsAuthorized;

/**
 * Account user whose school navigation returns protected domain objects.
 * For example, a controller uses this decorator for its authenticated principal.
 * Constructing the decorator does not verify a password or establish a login session.
 *
 * @since 0.0.1
 */
public final class UrAcAuthorized implements UrAccount {

    /** Acting account user. */
    private final UrAccount origin;

    /** School repository to protect. */
    private final Schools schools;

    /**
     * New user with protected school navigation.
     *
     * @param origin Acting account user
     * @param schools School repository
     */
    public UrAcAuthorized(final UrAccount origin, final Schools schools) {
        this.origin = origin;
        this.schools = schools;
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
        return new SlsAuthorized(this.schools, this.origin);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.origin.print(media);
    }
}
