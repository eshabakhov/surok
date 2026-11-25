/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.Users;
import com.eshabakhov.surok.federal.subject.SbsAuthorized;
import com.eshabakhov.surok.federal.subject.Subjects;
import com.eshabakhov.surok.school.building.BdsAuthorized;
import com.eshabakhov.surok.school.building.Buildings;
import com.eshabakhov.surok.school.schedule.Schedules;
import com.eshabakhov.surok.school.schedule.SdsAuthorized;
import com.eshabakhov.surok.school.schoolclass.SchoolClasses;
import com.eshabakhov.surok.school.schoolclass.ScsAuthorized;
import com.eshabakhov.surok.school.teacher.Teachers;
import com.eshabakhov.surok.school.teacher.ThsAuthorized;
import com.eshabakhov.surok.security.Operations;
import com.eshabakhov.surok.user.UrsAuthorized;

/**
 * School whose revision follows the acting user's current school role.
 * For example, {@code new SlAuthorized(school, student, operations).revise(input)} is
 * rejected, while a director can revise the same school.
 *
 * @since 0.0.1
 */
public final class SlAuthorized implements School {

    /** Shared operation declarations. */
    private final Operations operations;

    /** Underlying school. */
    private final School origin;

    /** Acting user. */
    private final UrAccount actor;

    /**
     * New protected object with shared operation declarations.
     *
     * @param origin Underlying origin
     * @param actor Acting user
     * @param operations Shared operation declarations
     */
    public SlAuthorized(
        final School origin, final UrAccount actor,
        final Operations operations
    ) {
        this.origin = origin;
        this.actor = actor;
        this.operations = operations;
    }

    @Override
    public Long uid() {
        return this.origin.uid();
    }

    @Override
    public School revise(final SchoolInput input) throws Exception {
        return new SlAuthorized(
            this.operations.operation("Schools.revise")
                .permission(this.actor, this.origin)
                .permit(this.origin).revise(input),
            this.actor, this.operations
        );
    }

    @Override
    public Users users() {
        return new UrsAuthorized(
            this.operations.operation("SchoolUsers.list")
                .permission(this.actor, this.origin)
                .permit(this.origin).users(), this.origin, this.actor, this.operations
        );
    }

    @Override
    public Buildings buildings() {
        return new BdsAuthorized(
            this.operations.operation("Buildings.selection").permission(this.actor, this.origin)
                .permit(this.origin).buildings(), this.origin, this.actor, this.operations
        );
    }

    @Override
    public Teachers teachers() {
        return new ThsAuthorized(
            this.operations.operation("Teachers.selection")
                .permission(this.actor, this.origin)
                .permit(this.origin).teachers(),
            this.origin, this.actor, this.operations
        );
    }

    @Override
    public SchoolClasses schoolClasses() {
        return new ScsAuthorized(
            this.operations.operation("SchoolClasses.selection")
                .permission(this.actor, this.origin)
                .permit(this.origin).schoolClasses(),
            this.origin, this.actor, this.operations
        );
    }

    @Override
    public Subjects subjects() {
        return new SbsAuthorized(
            this.operations.operation("SchoolSubjects.selection")
                .permission(this.actor, this.origin)
                .permit(this.origin).subjects(),
            this.origin, this.actor, this.operations
        );
    }

    @Override
    public Schedules schedules() {
        return new SdsAuthorized(
            this.operations.operation("Schedules.selection")
                .permission(this.actor, this.origin)
                .permit(this.origin).schedules(),
            this.origin, this.actor, this.operations
        );
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.operations.operation("Schools.school")
            .permission(this.actor, this.origin)
            .permit(this.origin).print(media);
    }
}
