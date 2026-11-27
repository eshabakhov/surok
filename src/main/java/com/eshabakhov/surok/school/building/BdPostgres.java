/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.building;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.school.building.cabinet.Cabinets;
import com.eshabakhov.surok.school.building.cabinet.CbsPostgres;
import com.eshabakhov.surok.school.building.input.MdBdInput;
import com.eshabakhov.surok.sql.ClasspathSql;
import org.jooq.DSLContext;
import org.jooq.Record;

/**
 * Postgres implementation of {@link Building}.
 *
 * @since 0.0.1
 */
public final class BdPostgres implements Building {

    /** JOOQ DSL context for executing database queries. */
    private final DSLContext ctx;

    /** Building id. */
    private final Long id;

    /**
     * New building.
     *
     * @param ctx Database context
     * @param id Building ID
     * @since 0.0.1
     */
    public BdPostgres(final DSLContext ctx, final Long id) {
        this.ctx = ctx;
        this.id = id;
    }

    @Override
    public Long uid() {
        return this.id;
    }

    @Override
    public Building revise(final BuildingInput input) throws Exception {
        final Long revised = this.ctx.resultQuery(
            new ClasspathSql("building/rename.sql").text(),
            input.print(new MdBdInput()).print().get("name"),
            this.id,
            false
        ).fetchOne("id", Long.class);
        if (revised == null) {
            throw new BuildingNotFoundException(
                String.format("Building with id=%d not found", this.id)
            );
        }
        return new BdPostgres(this.ctx, revised);
    }

    @Override
    public Cabinets cabinets() {
        return new CbsPostgres(this.ctx, this.id);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        final Record selected = this.ctx.resultQuery(
            new ClasspathSql("building/find-details.sql").text(), this.id, false
        ).fetchOne();
        if (selected == null) {
            throw new BuildingNotFoundException(
                String.format("Building with id=%d not found", this.id)
            );
        }
        return media.attribute("id", selected.get("id", Long.class))
            .with("name", selected.get("name", String.class));
    }
}
