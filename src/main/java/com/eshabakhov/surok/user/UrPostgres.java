/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Schools;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.school.SlsPostgres;
import org.jooq.DSLContext;

/**
 * Simple user implementation.
 *
 * @since 0.0.1
 */
public final class UrPostgres implements User {

    /** Database context. */
    private final DSLContext ctx;

    /** User ID. */
    private final long id;

    /**
     * New user.
     *
     * @param ctx Database context
     * @param id User identifier
     */
    public UrPostgres(final DSLContext ctx, final long id) {
        this.ctx = ctx;
        this.id = id;
    }

    @Override
    public long uid() {
        return this.id;
    }

    @Override
    public Credentials credentials() {
        return new Credentials.CdPostgres(this.ctx, this.id);
    }

    @Override
    public Roles roles() {
        return new Roles.RlsPostgres(this.ctx, this.id);
    }

    @Override
    public Schools schools() {
        return new SlsPostgres(this.ctx, this);
    }

    @Override
    public ReferenceInformation info() {
        return new ReferenceInformation.RefInfoPostgres(this.ctx, this.id);
    }

    @Override
    public boolean isAdmin() {
        return new Roles.RlsPostgres(this.ctx, this.id)
            .list()
            .stream()
            .anyMatch(role -> "ADMIN".equalsIgnoreCase(role.name()));
    }

    @Override
    public <T> Media<T> print(final Media<T> media) {
        return media
            .with("id", this.uid())
            .with("username", this.credentials().username())
            .with("admin", this.isAdmin());
    }
}
