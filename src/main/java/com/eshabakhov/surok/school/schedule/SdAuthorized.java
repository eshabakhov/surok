/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.school.schedule.load.ClassLoads;
import com.eshabakhov.surok.school.schedule.load.ClsAuthorized;
import com.eshabakhov.surok.school.schedule.load.autofill.LdAuthorized;
import com.eshabakhov.surok.school.schedule.load.autofill.LoadDraft;
import com.eshabakhov.surok.security.Operations;

/**
 * Schedule that checks the acting user's current school role.
 * For example, a student can read its loads but cannot revise its name.
 *
 * @since 0.0.1
 */
public final class SdAuthorized implements Schedule {

    /** Shared operation declarations. */
    private final Operations operations;

    /** Underlying schedule. */
    private final Schedule origin;

    /** Owning school. */
    private final School school;

    /** Acting user. */
    private final UrAccount actor;

    /**
     * New protected object with shared operation declarations.
     *
     * @param origin Underlying origin
     * @param school Underlying school
     * @param actor Acting user
     * @param operations Shared operation declarations
     */
    public SdAuthorized(
        final Schedule origin, final School school, final UrAccount actor,
        final Operations operations
    ) {
        this.origin = origin;
        this.school = school;
        this.actor = actor;
        this.operations = operations;
    }

    @Override
    public Long uid() {
        return this.origin.uid();
    }

    @Override
    public Schedule revise(final ScheduleInput input) throws Exception {
        return new SdAuthorized(
            this.operations.operation("Schedules.revise")
                .permission(this.actor, this.school)
                .permit(this.origin).revise(input),
            this.school, this.actor, this.operations
        );
    }

    @Override
    public ClassLoads loads() {
        return new ClsAuthorized(
            this.operations.operation("ClassLoads.selection")
                .permission(this.actor, this.school)
                .permit(this.origin).loads(),
            this.school, this.actor, this.operations
        );
    }

    @Override
    public LoadDraft draft() {
        return new LdAuthorized(
            this.operations.operation("LoadDraft.read")
                .permission(this.actor, this.school)
                .permit(this.origin).draft(),
            this.school, this.actor, this.operations
        );
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.operations.operation("Schedules.schedule")
            .permission(this.actor, this.school)
            .permit(this.origin).print(media);
    }
}
