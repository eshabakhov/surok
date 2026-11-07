/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Schools;
import com.eshabakhov.surok.User;

/**
 * Brief user representation for shared browser controls.
 *
 * @since 0.0.1
 */
public final class UrBrief implements User {

    /** Origin user. */
    private final User origin;

    /**
     * New brief representation.
     *
     * @param origin Origin user
     */
    public UrBrief(final User origin) {
        this.origin = origin;
    }

    @Override
    public long uid() {
        return this.origin.uid();
    }

    @Override
    public Credentials credentials() {
        return this.origin.credentials();
    }

    @Override
    public Roles roles() {
        return this.origin.roles();
    }

    @Override
    public Schools schools() {
        return this.origin.schools();
    }

    @Override
    public ReferenceInformation info() {
        return this.origin.info();
    }

    @Override
    public boolean isAdmin() {
        return this.origin.isAdmin();
    }

    @Override
    public <T> Media<T> print(final Media<T> media) {
        return media.with("username", this.credentials().username())
            .with("admin", this.isAdmin())
            .with("subscription", !this.info().corporate() && !this.isAdmin());
    }
}
