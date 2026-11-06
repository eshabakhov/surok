/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.sql.ClasspathSql;
import org.jooq.DSLContext;

/**
 * A user's role in one school.
 *
 * @since 0.0.1
 */
public final class RlSchool implements Role {

    /** Database context. */
    private final DSLContext ctx;

    /** Role assignment identifier. */
    private final long id;

    /**
     * New school role.
     *
     * @param ctx Database context
     * @param id Assignment identifier
     */
    public RlSchool(final DSLContext ctx, final long id) {
        this.ctx = ctx;
        this.id = id;
    }

    @Override
    public long uid() {
        return this.id;
    }

    @Override
    public String name() {
        return this.ctx.resultQuery(
            new ClasspathSql("school-user/role/name.sql").text(), this.id
        ).fetchOneInto(String.class);
    }
}
