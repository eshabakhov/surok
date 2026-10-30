/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.autofill;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.federal.curriculum.Curriculum;
import com.eshabakhov.surok.federal.subject.Subject;
import com.eshabakhov.surok.school.schedule.Schedule;
import com.eshabakhov.surok.school.schedule.load.hours.WeeklyHours;
import com.eshabakhov.surok.sql.ClasspathSql;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jooq.DSLContext;

/**
 * Grade additions read and replaced inside the owning draft's transaction.
 * For example, omitted saved grades are cleared even after their last class is archived.
 *
 * @since 0.0.1
 */
public final class AdsPostgres implements Additions {

    /** Transaction context. */
    private final DSLContext ctx;

    /** Owning schedule. */
    private final Schedule schedule;

    /** Selected curriculum. */
    private final Curriculum curriculum;

    /**
     * New collection of grade additions.
     *
     * @param ctx Transaction context
     * @param schedule Owning schedule
     * @param curriculum Selected curriculum
     */
    public AdsPostgres(
        final DSLContext ctx, final Schedule schedule, final Curriculum curriculum
    ) {
        this.ctx = ctx;
        this.schedule = schedule;
        this.curriculum = curriculum;
    }

    @Override
    public Iterable<Addition> iterate() throws Exception {
        return this.ctx.resultQuery(
            new ClasspathSql("load-addition/applicable.sql").text(),
            this.schedule.uid(), this.curriculum.grades().toArray(Integer[]::new)
        ).fetch(
            row -> new AdPostgres(
                this.ctx, this.schedule, this.curriculum, row.get("grade", Integer.class)
            )
        );
    }

    @Override
    public void save(final Map<Integer, Map<Subject, WeeklyHours>> additions) throws Exception {
        final List<Integer> grades = this.curriculum.grades();
        if (grades.isEmpty() || !grades.containsAll(additions.keySet())) {
            throw new LoadInputException(
                String.format("Invalid grade additions for curriculum %d", this.curriculum.uid())
            );
        }
        final Set<Integer> selected = new LinkedHashSet<>(additions.keySet());
        selected.addAll(
            this.ctx.resultQuery(
                new ClasspathSql("load-addition/grades.sql").text(), this.schedule.uid()
            ).fetch("grade", Integer.class)
        );
        for (final Integer grade : selected) {
            if (grades.contains(grade)) {
                new AdPostgres(this.ctx, this.schedule, this.curriculum, grade)
                    .save(additions.getOrDefault(grade, Map.of()));
            }
        }
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return new PrItems("addition", this.iterate()).print(media);
    }
}
