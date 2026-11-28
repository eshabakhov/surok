/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.Schools;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.school.SlsPostgres;
import com.eshabakhov.surok.security.OaYaml;
import java.io.IOException;
import org.jooq.DSLContext;

/**
 * Simple user implementation.
 *
 * @since 0.0.1
 */
public final class UrAcPostgres implements UrAccount {

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
    public UrAcPostgres(final DSLContext ctx, final long id) {
        this.ctx = ctx;
        this.id = id;
    }

    @Override
    public UrAccount permit(final String operation) throws IOException {
        return new OaYaml().operations().operation(operation).permission(this).permit(this);
    }

    @Override
    public UrAccount permit(final String operation, final School school) throws IOException {
        return new OaYaml().operations().operation(operation).permission(this, school).permit(this);
    }

    @Override
    public long uid() {
        return this.id;
    }

    @Override
    public Account account() {
        return new AcPostgres(this.ctx, this.id);
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
    public <T> Media<T> print(final Media<T> media) {
        return media
            .attribute("id", this.uid())
            .with("username", this.account().username())
            .with("admin", this.roles().contains(Roles.RoleEnum.ADMIN));
    }
}
