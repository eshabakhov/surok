/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.autofill;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.federal.curriculum.CrPostgres;
import com.eshabakhov.surok.federal.curriculum.Curriculum;
import com.eshabakhov.surok.federal.curriculum.requirement.RqMandatory;
import com.eshabakhov.surok.federal.curriculum.requirement.RqsPostgres;
import com.eshabakhov.surok.federal.subject.SbPostgres;
import com.eshabakhov.surok.federal.subject.Subject;
import com.eshabakhov.surok.school.schedule.Schedule;
import com.eshabakhov.surok.school.schedule.load.hours.WeeklyHours;
import com.eshabakhov.surok.school.schedule.load.hours.WhFixed;
import com.eshabakhov.surok.school.schoolclass.ScPostgres;
import com.eshabakhov.surok.sql.ClasspathSql;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

/**
 * PostgreSQL draft with automatic mandatory loads and manual grade additions.
 * For example, reopening includes newly created classes without saving a class selection.
 * Saving and application serialize on the owning schedule and roll back as a whole.
 *
 * @since 0.0.1
 */
public final class LdPostgres implements LoadDraft {

    /** Database context. */
    private final DSLContext ctx;

    /** Schedule owning the draft. */
    private final Schedule schedule;

    /**
     * New persistent draft.
     *
     * @param ctx Database context
     * @param schedule Owning schedule
     */
    public LdPostgres(final DSLContext ctx, final Schedule schedule) {
        this.ctx = ctx;
        this.schedule = schedule;
    }

    @Override
    public void save(
        final Curriculum curriculum,
        final Map<Integer, Map<Subject, WeeklyHours>> additions
    ) {
        this.ctx.transaction(
            config -> new LdPostgres(DSL.using(config), this.schedule)
                .replace(curriculum, additions)
        );
    }

    @Override
    public void revise(final Curriculum curriculum) {
        this.ctx.transaction(
            config -> {
                final LdPostgres draft = new LdPostgres(DSL.using(config), this.schedule);
                draft.lock();
                if (!draft.curriculum().uid().equals(curriculum.uid())) {
                    draft.replace(curriculum, Map.of());
                }
            }
        );
    }

    @Override
    public Curriculum curriculum() throws Exception {
        final Record selected = this.ctx.resultQuery(
            new ClasspathSql("load-draft/find.sql").text(), this.schedule.uid()
        ).fetchOne();
        if (selected == null) {
            throw new LoadDraftNotFoundException(this.schedule.uid());
        }
        return new CrPostgres(this.ctx, selected.get("curriculum_id", Long.class));
    }

    @Override
    @SuppressWarnings("PMD.PreserveStackTrace")
    public <T> Media<T> print(final Media<T> media) throws Exception {
        try {
            return this.ctx.transactionResult(
                config -> {
                    final LdPostgres draft = new LdPostgres(DSL.using(config), this.schedule);
                    draft.lock();
                    final Curriculum curriculum = draft.curriculum();
                    return media.with("curriculum", curriculum)
                        .with("loads", draft.prepared())
                        .with("additions", new AdsPostgres(draft.ctx, this.schedule, curriculum))
                        .with("requirements", new RqsPostgres(draft.ctx, curriculum.uid())).with(
                            "unavailable", new PrItems(
                                "requirement", draft.ctx.resultQuery(
                                    new ClasspathSql("load-draft/unavailable.sql").text(),
                                    this.schedule.uid(), curriculum.grades().toArray(Integer[]::new)
                                ).fetch(
                                    row -> new RqMandatory(draft.ctx, row.get("id", Long.class))
                                )
                            )
                        );
                }
            );
        } catch (final DataAccessException exception) {
            switch (exception.getCause()) {
                case LoadDraftNotFoundException absent -> throw absent;
                case null, default -> throw exception;
            }
        }
    }

    @Override
    public void apply() {
        this.ctx.transaction(
            config -> {
                final DSLContext transaction = DSL.using(config);
                final LdPostgres draft = new LdPostgres(transaction, this.schedule);
                draft.lock();
                transaction.resultQuery(
                    new ClasspathSql("load-draft/lock-loads.sql").text(), this.schedule.uid()
                ).fetch();
                for (final Addition addition : new AdsPostgres(
                    transaction, this.schedule, draft.curriculum()
                ).iterate()) {
                    addition.check();
                }
                final LtPostgres target = new LtPostgres(transaction, this.schedule);
                for (final PlannedLoad load : draft.prepared().iterate()) {
                    load.apply(target);
                }
            }
        );
    }

    /*
     * Saves the curriculum and complete grade selection inside the draft transaction.
     */
    private void replace(
        final Curriculum curriculum,
        final Map<Integer, Map<Subject, WeeklyHours>> additions
    ) throws Exception {
        this.lock();
        final Record previous = this.ctx.resultQuery(
            new ClasspathSql("load-draft/find.sql").text(), this.schedule.uid()
        ).fetchOne();
        if (previous != null
            && !curriculum.uid().equals(previous.get("curriculum_id", Long.class))) {
            this.ctx.execute(
                new ClasspathSql("load-addition/delete.sql").text(), this.schedule.uid()
            );
        }
        this.ctx.execute(
            new ClasspathSql("load-draft/save.sql").text(), this.schedule.uid(), curriculum.uid()
        );
        new AdsPostgres(
            this.ctx, this.schedule, new CrPostgres(this.ctx, curriculum.uid())
        ).save(additions);
    }

    /*
     * Builds the single application plan from mandatory requirements and saved additions.
     */
    private PlannedLoads prepared() throws Exception {
        final List<PlannedLoad> loads = new ArrayList<>(0);
        for (final Record row : this.ctx.resultQuery(
            new ClasspathSql("load-draft/preview.sql").text(),
            this.curriculum().grades().toArray(Integer[]::new), this.schedule.uid()
        ).fetch()) {
            PlannedLoad load = new PlInitial(
                new ScPostgres(this.ctx, row.get("school_class_id", Long.class)),
                new SbPostgres(this.ctx, row.get("subject_id", Long.class), true),
                new WhFixed(row.get("current", Integer.class)),
                new WhFixed(row.get("proposed", Integer.class))
            );
            if (row.get("proposed", Integer.class) > 0) {
                load = load.accept();
            } else if (row.get("planned") != null) {
                load = load.with(new WhFixed(row.get("planned", Integer.class)));
            }
            loads.add(load);
        }
        return new PlsMemory(loads);
    }

    /*
     * Locks the owning schedule for the surrounding draft transaction.
     */
    private void lock() throws Exception {
        if (this.ctx.resultQuery(
            new ClasspathSql("load-draft/lock.sql").text(), this.schedule.uid()
        ).fetch().isEmpty()) {
            throw new LoadDraftNotFoundException(this.schedule.uid());
        }
    }
}
