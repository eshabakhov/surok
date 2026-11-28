/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.teacher;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.school.teacher.input.MdThInput;
import com.eshabakhov.surok.sql.ClasspathSql;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

/**
 * Postgres school teacher.
 *
 * <p>Checked domain failures are unwrapped from transaction failures.</p>
 *
 * @since 0.0.1
 * @checkstyle LambdaBodyLengthCheck (100 lines)
 */
public final class ThPostgres implements Teacher {

    /** Database context. */
    private final DSLContext ctx;

    /** Teacher identifier. */
    private final Long id;

    /**
     * New teacher.
     *
     * @param ctx Database context
     * @param id Teacher identifier
     */
    public ThPostgres(final DSLContext ctx, final Long id) {
        this.ctx = ctx;
        this.id = id;
    }

    @Override
    public Long uid() {
        return this.id;
    }

    @Override
    @SuppressWarnings("PMD.PreserveStackTrace")
    public Teacher revise(final TeacherInput input) throws Exception {
        final String name = input.print(new MdThInput()).print().get("name");
        try {
            return this.ctx.transactionResult(
                config -> {
                    final DSLContext transaction = DSL.using(config);
                    final Record current = transaction.resultQuery(
                        new ClasspathSql("teacher/find-details.sql").text(), this.id, false
                    ).fetchOne();
                    if (current == null) {
                        throw new TeacherNotFoundException(
                            String.format("Teacher with id=%d not found", this.id)
                        );
                    }
                    final Record existing = transaction.resultQuery(
                        new ClasspathSql("teacher/find-existing.sql").text(),
                        current.get("school_id", Long.class), name, false
                    ).fetchOne();
                    if (existing != null && !this.id.equals(existing.get("id", Long.class))) {
                        throw new TeacherAlreadyExistsException(
                            String.format("Teacher `%s` already exists", name)
                        );
                    }
                    final Long revised = transaction.resultQuery(
                        new ClasspathSql("teacher/rename.sql").text(), name, this.id, false
                    ).fetchOne("id", Long.class);
                    if (revised == null) {
                        throw new TeacherNotFoundException(
                            String.format("Teacher with id=%d not found", this.id)
                        );
                    }
                    return new ThPostgres(this.ctx, revised);
                }
            );
        } catch (final DataAccessException exception) {
            switch (exception.getCause()) {
                case TeacherAlreadyExistsException duplicate -> throw duplicate;
                case TeacherNotFoundException missing -> throw missing;
                case null, default -> throw exception;
            }
        }
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        final Record selected = this.ctx.resultQuery(
            new ClasspathSql("teacher/find-details.sql").text(), this.id, false
        ).fetchOne();
        if (selected == null) {
            throw new TeacherNotFoundException(
                String.format("Teacher with id=%d not found", this.id)
            );
        }
        return media.attribute("id", selected.get("id", Long.class))
            .with("name", selected.get("name", String.class));
    }
}
