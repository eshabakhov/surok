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
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

/**
 * Subjects visible to a school, including read-only federal entries.
 * For example, {@code new SbsPostgres(ctx, school)} exposes shared and own subjects.
 *
 * <p>Checked domain failures are unwrapped from transaction failures.</p>
 *
 * @since 0.0.1
 * @checkstyle LambdaBodyLengthCheck (1000 lines)
 */
@EqualsAndHashCode
public final class SbsPostgres implements Subjects {

    /** JOOQ DSL context for executing database queries. */
    private final DSLContext ctx;

    /** School id. */
    private final Long sid;

    /**
     * New subjects collection.
     *
     * @param ctx Database context
     * @param sid School ID
     * @since 0.0.1
     */
    public SbsPostgres(final DSLContext ctx, final Long sid) {
        this.ctx = ctx;
        this.sid = sid;
    }

    @Override
    @SuppressWarnings("PMD.PreserveStackTrace")
    public Subject create(final SubjectInput input) throws Exception {
        final String name = input.print(new MdSbInput()).print().get("name");
        try {
            return this.ctx.transactionResult(
                config -> {
                    final DSLContext ttx = DSL.using(config);
                    if (ttx.resultQuery(
                        new ClasspathSql("subject/find-existing.sql").text(),
                        this.sid,
                        name,
                        false
                    ).fetchOne() == null) {
                        final Record created = ttx.resultQuery(
                            new ClasspathSql("subject/create.sql").text(),
                            this.sid,
                            name,
                            false
                        ).fetchOne();
                        if (created == null) {
                            throw new SubjectFailedCreateException();
                        }
                        return new SbPostgres(this.ctx, created.get("id", Long.class));
                    } else {
                        throw new SubjectAlreadyExistsException(
                            String.format("Subject `%s` already exists", name)
                        );
                    }
                }
            );
        } catch (final DataAccessException exception) {
            switch (exception.getCause()) {
                case SubjectAlreadyExistsException duplicate -> throw duplicate;
                case SubjectFailedCreateException failed -> throw failed;
                case null, default -> throw exception;
            }
        }
    }

    @Override
    public Subject subject(final long cid) throws Exception {
        final Record selected = this.ctx.resultQuery(
            new ClasspathSql("subject/find-by-id-and-school-id.sql").text(),
            cid,
            this.sid,
            false
        ).fetchOne();
        if (selected == null) {
            throw new SubjectNotFoundException(
                String.format("Subject with id=%d not found", cid)
            );
        }
        return new SbSchool(new SbPostgres(this.ctx, selected.get("id", Long.class)));
    }

    @Override
    public Subjects selection(
        final Filters filters,
        final Page page,
        final Sorts sorts
    ) throws Exception {
        final Condition scoped = new FlsCdSubject(filters).condition().and(
            DSL.condition(
                new ClasspathSql("subject/conditions/by-school-id-and-is-deleted.sql").text(),
                this.sid,
                false
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
            ).fetch(
                selected -> new SbSchool(new SbPostgres(this.ctx, selected.get("id", Long.class)))
            ),
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
            new ClasspathSql("subject/list-active.sql").text(), this.sid, false
        ).fetch(selected -> new SbSchool(new SbPostgres(this.ctx, selected.get("id", Long.class))));
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return media.with(new PrItems("subject", this.iterate()));
    }

    @Override
    public void remove(final long cid) throws Exception {
        if (this.subject(cid).origin() == Subject.Origin.FEDERAL) {
            throw new SubjectReadOnlyException(cid);
        }
        this.ctx.resultQuery(
            new ClasspathSql("subject/soft-delete.sql").text(), true, cid, this.sid
        ).execute();
    }
}
