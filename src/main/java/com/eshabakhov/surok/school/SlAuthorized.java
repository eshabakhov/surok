/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
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
import com.eshabakhov.surok.security.right.RtSchoolDirector;
import com.eshabakhov.surok.security.right.RtSchoolMember;
import com.eshabakhov.surok.user.UrsAuthorized;

/**
 * School whose revision follows the acting user's current school role.
 * For example, {@code new SlAuthorized(school, viewer).revise(input)} is
 * rejected, while a director can revise the same school.
 *
 * @since 0.0.1
 */
public final class SlAuthorized implements School {

    /** Underlying school. */
    private final School origin;

    /** Acting user. */
    private final User actor;

    /**
     * New authorized school.
     *
     * @param origin Underlying school
     * @param actor Acting user
     */
    public SlAuthorized(final School origin, final User actor) {
        this.origin = origin;
        this.actor = actor;
    }

    @Override
    public Long uid() {
        return this.origin.uid();
    }

    @Override
    public School revise(final SchoolInput input) throws Exception {
        return new SlAuthorized(
            new RtSchoolDirector(this.actor, this.origin).permit(this.origin).revise(input),
            this.actor
        );
    }

    @Override
    public Users users() {
        return new UrsAuthorized(
            new RtSchoolMember(this.actor, this.origin).permit(this.origin).users(), this.actor
        );
    }

    @Override
    public Buildings buildings() {
        return new BdsAuthorized(
            new RtSchoolMember(this.actor, this.origin).permit(this.origin).buildings(),
            this.origin, this.actor
        );
    }

    @Override
    public Teachers teachers() {
        return new ThsAuthorized(
            new RtSchoolMember(this.actor, this.origin).permit(this.origin).teachers(),
            this.origin, this.actor
        );
    }

    @Override
    public SchoolClasses schoolClasses() {
        return new ScsAuthorized(
            new RtSchoolMember(this.actor, this.origin).permit(this.origin).schoolClasses(),
            this.origin, this.actor
        );
    }

    @Override
    public Subjects subjects() {
        return new SbsAuthorized(
            new RtSchoolMember(this.actor, this.origin).permit(this.origin).subjects(),
            this.origin, this.actor
        );
    }

    @Override
    public Schedules schedules() {
        return new SdsAuthorized(
            new RtSchoolMember(this.actor, this.origin).permit(this.origin).schedules(),
            this.origin, this.actor
        );
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return new RtSchoolMember(this.actor, this.origin).permit(this.origin).print(media);
    }
}
