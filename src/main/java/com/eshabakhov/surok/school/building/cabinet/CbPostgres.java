/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.building.cabinet;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.school.building.cabinet.input.MdCbInput;
import com.eshabakhov.surok.sql.ClasspathSql;
import org.jooq.DSLContext;
import org.jooq.Record;

/**
 * Postgres implementation of {@link Cabinet}.
 *
 * @since 0.0.1
 */
public final class CbPostgres implements Cabinet {

    /** JOOQ DSL context for executing database queries. */
    private final DSLContext ctx;

    /** Cabinet id. */
    private final Long id;

    /**
     * New cabinet.
     *
     * @param ctx Database context
     * @param id Cabinet ID
     * @since 0.0.1
     */
    public CbPostgres(final DSLContext ctx, final Long id) {
        this.ctx = ctx;
        this.id = id;
    }

    @Override
    public Long uid() {
        return this.id;
    }

    @Override
    public Cabinet revise(final CabinetInput input) throws Exception {
        final Long revised = this.ctx.resultQuery(
            new ClasspathSql("cabinet/rename.sql").text(),
            input.print(new MdCbInput()).print().get("name"),
            this.id,
            false
        ).fetchOne("id", Long.class);
        if (revised == null) {
            throw new CabinetNotFoundException(
                String.format("Cabinet with id=%d not found", this.id)
            );
        }
        return new CbPostgres(this.ctx, revised);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        final Record selected = this.ctx.resultQuery(
            new ClasspathSql("cabinet/find-details.sql").text(), this.id, false
        ).fetchOne();
        if (selected == null) {
            throw new CabinetNotFoundException(
                String.format("Cabinet with id=%d not found", this.id)
            );
        }
        return media.attribute("id", selected.get("id", Long.class))
            .with("name", selected.get("name", String.class));
    }
}
