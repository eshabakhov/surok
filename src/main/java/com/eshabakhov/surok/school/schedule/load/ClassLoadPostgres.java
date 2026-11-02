/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.federal.subject.SbPostgres;
import com.eshabakhov.surok.school.schedule.load.hours.WhFixed;
import com.eshabakhov.surok.school.schoolclass.ScPostgres;
import com.eshabakhov.surok.sql.ClasspathSql;
import org.jooq.DSLContext;
import org.jooq.Record;

/**
 * Postgres teaching load, including names needed to display historical assignments.
 *
 * @since 0.0.1
 */
public final class ClassLoadPostgres implements ClassLoad {

    /** Database context. */
    private final DSLContext ctx;

    /** Load identifier. */
    private final Long lid;

    /**
     * New load.
     *
     * @param ctx Database context
     * @param lid Load identifier
     */
    public ClassLoadPostgres(final DSLContext ctx, final Long lid) {
        this.ctx = ctx;
        this.lid = lid;
    }

    @Override
    public Long uid() {
        return this.lid;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        final Record selected = this.details();
        return new WhFixed(selected.get("hours", Integer.class)).print(
            media.with("id", selected.get("id", Long.class)).with(
                "schoolClass", new ScPostgres(
                    this.ctx, selected.get("school_class_id", Long.class), true
                )
            ).with(
                "subject", new SbPostgres(this.ctx, selected.get("subject_id", Long.class), true)
            )
        );
    }

    private Record details() throws Exception {
        final Record selected = this.ctx.resultQuery(
            new ClasspathSql("class-load/find-details.sql").text(), this.lid
        ).fetchOne();
        if (selected == null) {
            throw new LoadNotFoundException(
                String.format("Class load with id=%d not found", this.lid)
            );
        }
        return selected;
    }
}
