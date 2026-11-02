/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.Users;
import com.eshabakhov.surok.federal.subject.SbsPostgres;
import com.eshabakhov.surok.federal.subject.Subjects;
import com.eshabakhov.surok.school.building.BdsPostgres;
import com.eshabakhov.surok.school.building.Buildings;
import com.eshabakhov.surok.school.input.MdSlInput;
import com.eshabakhov.surok.school.schedule.Schedules;
import com.eshabakhov.surok.school.schedule.SdsPostgres;
import com.eshabakhov.surok.school.schoolclass.SchoolClasses;
import com.eshabakhov.surok.school.schoolclass.ScsPostgres;
import com.eshabakhov.surok.school.teacher.Teachers;
import com.eshabakhov.surok.school.teacher.ThsPostgres;
import com.eshabakhov.surok.sql.ClasspathSql;
import com.eshabakhov.surok.user.UrsPostgres;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jooq.DSLContext;
import org.jooq.Record;

/**
 * Postgres implementation of {@link School}.
 *
 * @since 0.0.1
 */
@ToString(of = "sid")
@EqualsAndHashCode
public final class SlPostgres implements School {

    /** JOOQ DSL context for executing database queries. */
    private final DSLContext ctx;

    /** School id. */
    private final Long sid;

    /**
     * New school.
     *
     * @param ctx Database context
     * @param sid School ID
     * @since 0.0.1
     */
    public SlPostgres(final DSLContext ctx, final Long sid) {
        this.ctx = ctx;
        this.sid = sid;
    }

    @Override
    public Long uid() {
        return this.sid;
    }

    @Override
    public School revise(final SchoolInput input) throws Exception {
        final Record revised = this.ctx.resultQuery(
            new ClasspathSql("school/revise.sql").text(),
            input.print(new MdSlInput()).print().get("name"), this.sid, false
        ).fetchOne();
        if (revised == null) {
            throw new SchoolNotFoundException(
                String.format("School with id=%d not found", this.sid)
            );
        }
        return new SlPostgres(this.ctx, revised.get("id", Long.class));
    }

    @Override
    public Users users() {
        return new UrsPostgres(this.ctx, this.sid);
    }

    @Override
    public Buildings buildings() {
        return new BdsPostgres(this.ctx, this.sid);
    }

    @Override
    public Teachers teachers() {
        return new ThsPostgres(this.ctx, this.sid);
    }

    @Override
    public SchoolClasses schoolClasses() {
        return new ScsPostgres(this.ctx, this.sid);
    }

    @Override
    public Subjects subjects() {
        return new SbsPostgres(this.ctx, this.sid);
    }

    @Override
    public Schedules schedules() {
        return new SdsPostgres(this.ctx, this.sid);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        final Record selected = this.ctx.resultQuery(
            new ClasspathSql("school/find-details.sql").text(), this.sid, false
        ).fetchOne();
        if (selected == null) {
            throw new SchoolNotFoundException(
                String.format("School with id=%d not found", this.sid)
            );
        }
        return media.with("id", selected.get("id", Long.class))
            .with("name", selected.get("name", String.class));
    }
}
