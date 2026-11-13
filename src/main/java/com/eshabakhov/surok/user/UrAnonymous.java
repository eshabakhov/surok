/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Schools;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.school.SlsFixed;
import java.util.List;

/**
 * Visitor without an authenticated account, assigned roles, or schools.
 * For example, {@code new UrAnonymous().roles().contains(Roles.RoleEnum.ADMIN)} is false.
 *
 * @since 0.0.1
 */
public final class UrAnonymous implements User {

    /** Account without credentials or authentication. */
    private final Account details;

    /** Empty account roles. */
    private final Roles assigned;

    /** Empty school collection. */
    private final Schools available;

    /**
     * New anonymous visitor.
     */
    public UrAnonymous() {
        this(new AcAnonymous(), new RlsFixed(List.of()), new SlsFixed(List.of()));
    }

    /**
     * New anonymous visitor with immutable empty collections.
     *
     * @param details Anonymous account
     * @param assigned Empty roles
     * @param available Empty schools
     */
    private UrAnonymous(final Account details, final Roles assigned, final Schools available) {
        this.details = details;
        this.assigned = assigned;
        this.available = available;
    }

    @Override
    public long uid() {
        throw new IllegalStateException("An anonymous visitor has no account identifier");
    }

    @Override
    public Account account() {
        return this.details;
    }

    @Override
    public Roles roles() {
        return this.assigned;
    }

    @Override
    public Schools schools() {
        return this.available;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) {
        return media.with("authenticated", this.account().authenticated()).with("admin", false);
    }
}
