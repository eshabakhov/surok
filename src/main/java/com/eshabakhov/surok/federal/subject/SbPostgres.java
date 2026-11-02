/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.federal.subject.input.MdSbInput;
import com.eshabakhov.surok.sql.ClasspathSql;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

/**
 * Postgres subject, for example a common federal subject or a school addition.
 *
 * <p>Checked domain failures are unwrapped from transaction failures.</p>
 *
 * @since 0.0.1
 * @checkstyle LambdaBodyLengthCheck (100 lines)
 */
public final class SbPostgres implements Subject {

    /** Database context. */
    private final DSLContext ctx;

    /** Subject identifier. */
    private final Long id;

    /** Whether archived records are readable. */
    private final boolean archived;

    /**
     * New subject.
     *
     * @param ctx Database context
     * @param id Subject identifier
     */
    public SbPostgres(final DSLContext ctx, final Long id) {
        this(ctx, id, false);
    }

    /**
     * New subject with optional access to historical records.
     *
     * @param ctx Database context
     * @param id Entity identifier
     * @param archived Whether archived records are readable
     */
    public SbPostgres(final DSLContext ctx, final Long id, final boolean archived) {
        this.ctx = ctx;
        this.id = id;
        this.archived = archived;
    }

    @Override
    public Long uid() {
        return this.id;
    }

    @Override
    public String name() throws Exception {
        return this.details().get("name", String.class);
    }

    @Override
    public Origin origin() throws Exception {
        return Origin.valueOf(this.details().get("origin", String.class));
    }

    @Override
    @SuppressWarnings("PMD.PreserveStackTrace")
    public Subject revise(final SubjectInput input) throws Exception {
        final String name = input.print(new MdSbInput()).print().get("name");
        try {
            return this.ctx.transactionResult(
                config -> {
                    final DSLContext transaction = DSL.using(config);
                    if (transaction.resultQuery(
                        new ClasspathSql("subject/find-details.sql").text(), this.id, false
                    ).fetchOne() == null) {
                        throw new SubjectNotFoundException(
                            String.format("Subject with id=%d not found", this.id)
                        );
                    }
                    final Record existing = transaction.resultQuery(
                        new ClasspathSql("subject/find-duplicate.sql").text(),
                        this.id, name, false
                    ).fetchOne();
                    if (existing != null && !this.id.equals(existing.get("id", Long.class))) {
                        throw new SubjectAlreadyExistsException(
                            String.format("Subject `%s` already exists", name)
                        );
                    }
                    final Long revised = transaction.resultQuery(
                        new ClasspathSql("subject/rename.sql").text(), name, this.id, false
                    ).fetchOne("id", Long.class);
                    if (revised == null) {
                        throw new SubjectNotFoundException(
                            String.format("Subject with id=%d not found", this.id)
                        );
                    }
                    return new SbPostgres(this.ctx, revised);
                }
            );
        } catch (final DataAccessException exception) {
            switch (exception.getCause()) {
                case SubjectAlreadyExistsException duplicate -> throw duplicate;
                case SubjectNotFoundException missing -> throw missing;
                case null, default -> throw exception;
            }
        }
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        final Record selected = this.details();
        return media.with("id", selected.get("id", Long.class))
            .with("name", selected.get("name", String.class))
            .with("origin", selected.get("origin", String.class));
    }

    private Record details() throws Exception {
        final Record selected = this.ctx.resultQuery(
            new ClasspathSql("subject/find-details.sql").text(), this.id, this.archived
        ).fetchOne();
        if (selected == null) {
            throw new SubjectNotFoundException(
                String.format("Subject with id=%d not found", this.id)
            );
        }
        return selected;
    }
}
