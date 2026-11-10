/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.school.schedule.load.ClassLoads;
import com.eshabakhov.surok.school.schedule.load.ClsAuthorized;
import com.eshabakhov.surok.school.schedule.load.autofill.LdAuthorized;
import com.eshabakhov.surok.school.schedule.load.autofill.LoadDraft;
import com.eshabakhov.surok.security.right.RtAdmin;
import com.eshabakhov.surok.security.right.RtDirectorOrDeputy;
import com.eshabakhov.surok.security.right.RtEither;
import com.eshabakhov.surok.security.right.RtSchoolMember;

/**
 * Schedule that checks the acting user's current school role.
 * For example, a viewer can read its loads but cannot revise its name.
 *
 * @since 0.0.1
 */
public final class SdAuthorized implements Schedule {

    /** Underlying schedule. */
    private final Schedule origin;

    /** Owning school. */
    private final School school;

    /** Acting user. */
    private final User actor;

    /**
     * New protected schedule.
     *
     * @param origin Underlying schedule
     * @param school Owning school
     * @param actor Acting user
     */
    public SdAuthorized(final Schedule origin, final School school, final User actor) {
        this.origin = origin;
        this.school = school;
        this.actor = actor;
    }

    @Override
    public Long uid() {
        return this.origin.uid();
    }

    @Override
    public Schedule revise(final ScheduleInput input) throws Exception {
        return new SdAuthorized(
            new RtEither(
                new RtAdmin(this.actor), new RtDirectorOrDeputy(this.actor, this.school)
            ).permit(this.origin).revise(input),
            this.school, this.actor
        );
    }

    @Override
    public ClassLoads loads() {
        return new ClsAuthorized(
            new RtEither(
                new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
            ).permit(this.origin).loads(),
            this.school, this.actor
        );
    }

    @Override
    public LoadDraft draft() {
        return new LdAuthorized(
            new RtEither(
                new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
            ).permit(this.origin).draft(),
            this.school, this.actor
        );
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return new RtEither(
            new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
        ).permit(this.origin).print(media);
    }
}
