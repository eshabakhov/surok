/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.federal.curriculum.input.MdCrInput;
import com.eshabakhov.surok.federal.curriculum.requirement.Requirements;
import com.eshabakhov.surok.federal.curriculum.requirement.RqsPostgres;
import com.eshabakhov.surok.sql.ClasspathSql;
import java.util.List;
import java.util.Map;
import org.jooq.DSLContext;

/**
 * Postgres implementation of {@link Curriculum}.
 * Holds only {@code fid} and {@code ctx} - all data is read from DB on demand.
 *
 * @since 0.0.1
 */
public final class CrPostgres implements Curriculum {

    /**
     * Federal curriculum id.
     */
    private final Long fid;

    /**
     * Database connection.
     */
    private final DSLContext ctx;

    /**
     * Creates a Postgres-backed curriculum.
     *
     * @param ctx JOOQ DSL context
     * @param fid Curriculum ID
     */
    public CrPostgres(final DSLContext ctx, final Long fid) {
        this.ctx = ctx;
        this.fid = fid;
    }

    @Override
    public List<Integer> grades() throws Exception {
        return List.copyOf(
            this.ctx.resultQuery(
                new ClasspathSql("curriculum/grades.sql").text(), this.fid
            ).fetch("grade", Integer.class)
        );
    }

    @Override
    public Long uid() {
        return this.fid;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.ctx.resultQuery(
            new ClasspathSql("curriculum/find-details.sql").text(),
            this.fid
        ).fetchSingle(
            record -> media
                .with("id", record.get("id", Long.class))
                .with("title", record.get("title", String.class))
                .with("level", record.get("education_level", String.class))
                .with("week", record.get("study_week_type", String.class))
                .with("version", record.get("version", String.class))
                .with("year", record.get("academic_year", String.class))
                .with("description", record.get("description", String.class))
        );
    }

    @Override
    public Curriculum revise(final CurriculumInput input) throws Exception {
        final Map<String, String> fields = input.print(
            new MdCrInput()
        ).print();
        return new CrPostgres(
            this.ctx,
            this.ctx.resultQuery(
                new ClasspathSql("curriculum/revise.sql").text(),
                fields.get("title"),
                Curriculum.Level.valueOf(fields.get("level")).name(),
                Curriculum.Week.valueOf(fields.get("week")).name(),
                fields.get("version"),
                fields.get("year"),
                fields.get("description"),
                this.fid
            ).fetchOne("id", Long.class)
        );
    }

    @Override
    public Requirements requirements() {
        return new RqsPostgres(this.ctx, this.fid);
    }
}
