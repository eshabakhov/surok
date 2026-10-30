/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.autofill;

import com.eshabakhov.surok.federal.subject.Subject;
import com.eshabakhov.surok.school.schedule.Schedule;
import com.eshabakhov.surok.school.schedule.load.hours.WeeklyHours;
import com.eshabakhov.surok.school.schoolclass.SchoolClass;
import com.eshabakhov.surok.sql.ClasspathSql;
import org.jooq.DSLContext;

/**
 * Actual loads written within a locked draft application transaction.
 * For example, {@code load.apply(new LtPostgres(transaction, schedule))} updates one pair.
 * Only the owning school's active classes are writable; historical subjects
 * can be removed or updated while their actual load still exists.
 * Instances are used only inside {@link LdPostgres}'s application boundary.
 *
 * @since 0.0.1
 */
final class LtPostgres implements LoadTarget {

    /** Transaction context. */
    private final DSLContext ctx;

    /** Locked schedule. */
    private final Schedule schedule;

    /**
     * New transaction-bound load destination.
     *
     * @param ctx Transaction context
     * @param schedule Locked schedule
     */
    LtPostgres(final DSLContext ctx, final Schedule schedule) {
        this.ctx = ctx;
        this.schedule = schedule;
    }

    @Override
    public void write(final SchoolClass clazz, final Subject subject, final WeeklyHours hours)
        throws Exception {
        final int value = hours.value();
        if (this.ctx.resultQuery(
            new ClasspathSql("load-draft/validate-load.sql").text(),
            this.schedule.uid(), clazz.uid(), subject.uid(), value
        ).fetch().isEmpty()) {
            throw new LoadInputException(
                String.format(
                    "Cannot apply hours to class %d and subject %d in schedule %d",
                    clazz.uid(), subject.uid(), this.schedule.uid()
                )
            );
        }
        if (value == 0) {
            this.ctx.execute(
                new ClasspathSql("load-draft/remove-load.sql").text(),
                this.schedule.uid(), clazz.uid(), subject.uid()
            );
        } else {
            this.ctx.execute(
                new ClasspathSql("load-draft/write-load.sql").text(),
                this.schedule.uid(), clazz.uid(), subject.uid(), value
            );
        }
    }
}
