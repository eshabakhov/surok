/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.sql.ClasspathSql;
import org.jooq.DSLContext;

/**
 * Account details of an identified application user stored in PostgreSQL.
 * For example, {@code new AcPostgres(ctx, user.uid()).alive()} rejects a deleted account.
 *
 * @since 0.0.1
 */
public final class AcPostgres implements Account {

    /** Database context. */
    private final DSLContext ctx;

    /** User identifier. */
    private final long user;

    /**
     * New account belonging to an identified application user.
     *
     * @param ctx Database context
     * @param user User identifier
     */
    public AcPostgres(final DSLContext ctx, final long user) {
        this.ctx = ctx;
        this.user = user;
    }

    @Override
    public boolean authenticated() {
        return true;
    }

    @Override
    public String username() {
        return this.ctx.resultQuery(
            new ClasspathSql("user/find-username.sql").text(), this.user
        ).fetchOneInto(String.class);
    }

    @Override
    public String password() {
        return this.ctx.resultQuery(
            new ClasspathSql("user/find-password.sql").text(), this.user
        ).fetchOneInto(String.class);
    }

    @Override
    public boolean corporate() {
        return this.ctx.resultQuery(
            new ClasspathSql("user/find-corporate.sql").text(), this.user
        ).fetchOneInto(Boolean.class);
    }

    @Override
    public boolean alive() {
        return Boolean.TRUE.equals(
            this.ctx.resultQuery(
                new ClasspathSql("user/find-alive.sql").text(), this.user
            ).fetchOneInto(Boolean.class)
        );
    }
}
