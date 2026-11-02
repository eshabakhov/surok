/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.autofill;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.PrFields;
import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.federal.curriculum.Curriculum;
import com.eshabakhov.surok.federal.curriculum.requirement.RqsPostgres;
import com.eshabakhov.surok.federal.subject.SbPostgres;
import com.eshabakhov.surok.federal.subject.Subject;
import com.eshabakhov.surok.school.schedule.Schedule;
import com.eshabakhov.surok.school.schedule.load.hours.WeeklyHours;
import com.eshabakhov.surok.school.schedule.load.hours.WhFixed;
import com.eshabakhov.surok.school.schedule.load.hours.WhPlanned;
import com.eshabakhov.surok.school.schedule.load.hours.WhProposed;
import com.eshabakhov.surok.school.schoolclass.ScPostgres;
import com.eshabakhov.surok.sql.ClasspathSql;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.EqualsAndHashCode;
import org.jooq.DSLContext;
import org.jooq.Record;

/**
 * One grade's additional selections and class totals inside a locked draft transaction.
 * For example, checking counts existing individual loads together with saved proposals.
 * Printing remains possible after the curriculum maximum is reduced below saved hours.
 *
 * @since 0.0.1
 */
@EqualsAndHashCode
public final class AdPostgres implements Addition {

    /** Transaction context. */
    private final DSLContext ctx;

    /** Owning schedule. */
    private final Schedule schedule;

    /** Selected curriculum. */
    private final Curriculum curriculum;

    /** Grade shared by the affected classes. */
    private final int grade;

    /**
     * New additional load for one grade.
     *
     * @param ctx Transaction context
     * @param schedule Owning schedule
     * @param curriculum Selected curriculum
     * @param grade Grade of the affected classes
     */
    public AdPostgres(
        final DSLContext ctx, final Schedule schedule, final Curriculum curriculum, final int grade
    ) {
        this.ctx = ctx;
        this.schedule = schedule;
        this.curriculum = curriculum;
        this.grade = grade;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return media.with("grade", this.grade).with(
            "requirements", new PrItems(
                "requirement", new RqsPostgres(this.ctx, this.curriculum.uid()).optional(this.grade)
            )
        ).with(
            "subjects", new PrItems(
                "subject", this.selected().stream().map(
                    row -> new PrFields(
                        List.of(new SbPostgres(this.ctx, row.get("subject_id", Long.class))),
                        Map.of("hours", row.get("hours", Integer.class)), Map.of()
                    )
                ).toList()
            )
        ).with(
            "classes", new PrItems(
                "class", this.usage().stream().map(
                    row -> new PrFields(
                        List.of(new ScPostgres(this.ctx, row.get("school_class_id", Long.class))),
                        Map.of(), Map.of(
                            "hours", new WhPlanned(
                                new WhProposed(
                                    new WhFixed(row.get("current", Integer.class)), new WhFixed(0)
                                ),
                                new WhFixed(row.get("planned", Integer.class))
                            )
                        )
                    )
                ).toList()
            )
        );
    }

    @Override
    public void save(final Map<Subject, WeeklyHours> hours) throws Exception {
        final Map<Long, Integer> selected = new LinkedHashMap<>();
        for (final Map.Entry<Subject, WeeklyHours> entry : hours.entrySet()) {
            if (selected.putIfAbsent(entry.getKey().uid(), entry.getValue().value()) != null) {
                throw new LoadInputException(
                    String.format("Duplicate additional subject %d", entry.getKey().uid())
                );
            }
        }
        this.ctx.execute(
            new ClasspathSql("load-addition/clear.sql").text(), this.schedule.uid(), this.grade
        );
        for (final Map.Entry<Long, Integer> entry : selected.entrySet()) {
            if (this.ctx.execute(
                new ClasspathSql("load-addition/save.sql").text(),
                this.grade, entry.getValue(), this.schedule.uid(), entry.getKey()
            ) != 1) {
                throw new LoadInputException(
                    String.format(
                        "Additional subject %d is unavailable in the school of schedule %d",
                        entry.getKey(), this.schedule.uid()
                    )
                );
            }
        }
        this.check();
    }

    @Override
    public void check() throws Exception {
        int maximum = 0;
        for (final Record row : this.selected()) {
            final int hours = row.get("hours", Integer.class);
            if (!row.get("owned", Boolean.class)
                || hours > 0 && !row.get("available", Boolean.class)) {
                throw new LoadInputException(
                    String.format(
                        "Additional subject %d is no longer available for schedule %d",
                        row.get("subject_id", Long.class), this.schedule.uid()
                    )
                );
            }
            maximum = Math.addExact(maximum, hours);
        }
        for (final Record row : this.usage()) {
            maximum = Math.max(maximum, row.get("planned", Integer.class));
        }
        if (maximum > 0) {
            new RqsPostgres(this.ctx, this.curriculum.uid()).check(this.grade, maximum);
        }
    }

    /*
     * Reads saved selections, including pending removals of archived subjects.
     */
    private List<Record> selected() throws Exception {
        return this.ctx.resultQuery(
            new ClasspathSql("load-addition/list.sql").text(), this.schedule.uid(), this.grade
        ).fetch();
    }

    /*
     * Reads current and planned additional totals, including individual manual loads.
     */
    private List<Record> usage() throws Exception {
        return this.ctx.resultQuery(
            new ClasspathSql("load-addition/usage.sql").text(), this.grade, this.schedule.uid()
        ).fetch();
    }
}
