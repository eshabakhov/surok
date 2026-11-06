/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.school.schedule.input.MdSdInput;
import com.eshabakhov.surok.school.schedule.load.ClassLoads;
import com.eshabakhov.surok.school.schedule.load.ClassLoadsPostgres;
import com.eshabakhov.surok.school.schedule.load.autofill.LdPostgres;
import com.eshabakhov.surok.school.schedule.load.autofill.LoadDraft;
import com.eshabakhov.surok.sql.ClasspathSql;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

/**
 * Postgres school schedule.
 *
 * <p>Checked domain failures are unwrapped from transaction failures.</p>
 *
 * @since 0.0.1
 * @checkstyle LambdaBodyLengthCheck (100 lines)
 */
public final class SdPostgres implements Schedule {

    /** Database context. */
    private final DSLContext ctx;

    /** Schedule identifier. */
    private final Long id;

    /**
     * New schedule.
     *
     * @param ctx Database context
     * @param id Schedule identifier
     */
    public SdPostgres(final DSLContext ctx, final Long id) {
        this.ctx = ctx;
        this.id = id;
    }

    @Override
    public Long uid() {
        return this.id;
    }

    @Override
    @SuppressWarnings("PMD.PreserveStackTrace")
    public Schedule revise(final ScheduleInput input) throws Exception {
        final String name = input.print(new MdSdInput()).print().get("name");
        try {
            return this.ctx.transactionResult(
                config -> {
                    final DSLContext transaction = DSL.using(config);
                    final Record current = transaction.resultQuery(
                        new ClasspathSql("schedule/find-details.sql").text(), this.id, false
                    ).fetchOne();
                    if (current == null) {
                        throw new ScheduleNotFoundException(
                            String.format("Schedule with id=%d not found", this.id)
                        );
                    }
                    final Record existing = transaction.resultQuery(
                        new ClasspathSql("schedule/find-existing.sql").text(),
                        current.get("school_id", Long.class), name, false
                    ).fetchOne();
                    if (existing != null && !this.id.equals(existing.get("id", Long.class))) {
                        throw new ScheduleAlreadyExistsException(
                            String.format("Schedule `%s` already exists", name)
                        );
                    }
                    final Long revised = transaction.resultQuery(
                        new ClasspathSql("schedule/rename.sql").text(), name, this.id, false
                    ).fetchOne("id", Long.class);
                    if (revised == null) {
                        throw new ScheduleNotFoundException(
                            String.format("Schedule with id=%d not found", this.id)
                        );
                    }
                    return new SdPostgres(this.ctx, revised);
                }
            );
        } catch (final DataAccessException exception) {
            switch (exception.getCause()) {
                case ScheduleAlreadyExistsException duplicate -> throw duplicate;
                case ScheduleNotFoundException missing -> throw missing;
                case null, default -> throw exception;
            }
        }
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        final Record selected = this.ctx.resultQuery(
            new ClasspathSql("schedule/find-details.sql").text(), this.id, false
        ).fetchOne();
        if (selected == null) {
            throw new ScheduleNotFoundException(
                String.format("Schedule with id=%d not found", this.id)
            );
        }
        return media.with("id", selected.get("id", Long.class))
            .with("name", selected.get("name", String.class));
    }

    @Override
    public ClassLoads loads() {
        return new ClassLoadsPostgres(this.ctx, this.id);
    }

    @Override
    public LoadDraft draft() {
        return new LdPostgres(this.ctx, this);
    }
}
