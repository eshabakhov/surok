/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.federal.subject.filter.FlsCdSubject;
import com.eshabakhov.surok.federal.subject.input.MdSbInput;
import com.eshabakhov.surok.federal.subject.sort.StsJqSubject;
import com.eshabakhov.surok.page.ResponsePage;
import com.eshabakhov.surok.sql.ClasspathSql;
import lombok.EqualsAndHashCode;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;

/**
 * Shared federal subject catalogue.
 * For example, {@code new SbsFederal(ctx).create(input)} makes a subject visible to every school.
 *
 * @since 0.0.1
 */
@EqualsAndHashCode
public final class SbsFederal implements Subjects {

    /** JOOQ DSL context for executing database queries. */
    private final DSLContext ctx;

    /**
     * New subjects collection.
     *
     * @param ctx Database context
     * @since 0.0.1
     */
    public SbsFederal(final DSLContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Subject create(final SubjectInput input) throws Exception {
        final String name = input.print(new MdSbInput()).print().get("name");
        final Record created = this.ctx.resultQuery(
            new ClasspathSql("subject/create-federal.sql").text(), name
        ).fetchOne();
        if (created == null) {
            throw new SubjectAlreadyExistsException(
                String.format("Federal subject `%s` already exists", name)
            );
        }
        return new SbPostgres(this.ctx, created.get("id", Long.class));
    }

    @Override
    public Subject subject(final long cid) throws Exception {
        final Record selected = this.ctx.resultQuery(
            new ClasspathSql("subject/find-federal.sql").text(), cid, false
        ).fetchOne();
        if (selected == null) {
            throw new SubjectNotFoundException(
                String.format("Subject with id=%d not found", cid)
            );
        }
        return new SbPostgres(this.ctx, selected.get("id", Long.class));
    }

    @Override
    public Subjects selection(
        final Filters filters,
        final Page page,
        final Sorts sorts
    ) throws Exception {
        final Condition scoped = new FlsCdSubject(filters).condition().and(
            DSL.condition(
                new ClasspathSql("subject/conditions/federal.sql").text(), false
            )
        );
        return new SbsSelected(
            this,
            this.ctx.resultQuery(
                new ClasspathSql("subject/list.sql").text(),
                scoped,
                DSL.list(new StsJqSubject(sorts).fields()),
                DSL.val(page.limit()),
                DSL.val((page.offset() - 1) * page.limit())
            ).fetch(selected -> new SbPostgres(this.ctx, selected.get("id", Long.class))),
            new ResponsePage(
                page,
                this.ctx.resultQuery(
                    new ClasspathSql("subject/count.sql").text(), scoped
                ).fetchSingle(0, Integer.class)
            )
        );
    }

    @Override
    public Iterable<Subject> iterate() {
        return this.ctx.resultQuery(
            new ClasspathSql("subject/list-federal.sql").text(), false
        ).fetch(selected -> new SbPostgres(this.ctx, selected.get("id", Long.class)));
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return media.with(new PrItems("subject", this.iterate()));
    }

    @Override
    public void remove(final long cid) throws Exception {
        this.subject(cid);
        this.ctx.resultQuery(
            new ClasspathSql("subject/delete-federal.sql").text(), true, cid
        ).execute();
    }
}
