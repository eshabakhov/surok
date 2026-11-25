/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Schools;
import com.eshabakhov.surok.UrAccount;

/**
 * Brief user representation for shared browser controls.
 *
 * @since 0.0.1
 */
public final class UrAcBrief implements UrAccount {

    /** Origin user. */
    private final UrAccount origin;

    /**
     * New brief representation.
     *
     * @param origin Origin user
     */
    public UrAcBrief(final UrAccount origin) {
        this.origin = origin;
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
        return this.origin.schools();
    }

    @Override
    public <T> Media<T> print(final Media<T> media) {
        return media.with("username", this.account().username())
            .with("admin", this.roles().contains(Roles.RoleEnum.ADMIN)).with(
                "subscription", !this.account().corporate()
                    && !this.roles().contains(Roles.RoleEnum.ADMIN)
            );
    }
}
