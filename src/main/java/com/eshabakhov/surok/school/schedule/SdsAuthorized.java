/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.security.right.RtAdmin;
import com.eshabakhov.surok.security.right.RtDirectorOrDeputy;
import com.eshabakhov.surok.security.right.RtEither;
import com.eshabakhov.surok.security.right.RtSchoolMember;
import java.util.stream.StreamSupport;

/**
 * School schedules protected by the acting user's school role.
 * For example, a director can create a schedule and a viewer can list them.
 *
 * @since 0.0.1
 */
public final class SdsAuthorized implements Schedules {

    /** Underlying schedules. */
    private final Schedules origin;

    /** Owning school. */
    private final School school;

    /** Acting user. */
    private final User actor;

    /**
     * New protected schedules.
     *
     * @param origin Underlying schedules
     * @param school Owning school
     * @param actor Acting user
     */
    public SdsAuthorized(final Schedules origin, final School school, final User actor) {
        this.origin = origin;
        this.school = school;
        this.actor = actor;
    }

    @Override
    public Schedule create(final ScheduleInput input) throws Exception {
        return new SdAuthorized(
            new RtEither(
                new RtAdmin(this.actor), new RtDirectorOrDeputy(this.actor, this.school)
            ).permit(this.origin).create(input),
            this.school, this.actor
        );
    }

    @Override
    public Schedule schedule(final long id) throws Exception {
        return new SdAuthorized(
            new RtEither(
                new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
            ).permit(this.origin).schedule(id),
            this.school, this.actor
        );
    }

    @Override
    public Schedules selection(
        final Filters filters, final Page page, final Sorts sorts
    ) throws Exception {
        return new SdsAuthorized(
            new RtEither(
                new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
            ).permit(this.origin).selection(filters, page, sorts),
            this.school, this.actor
        );
    }

    @Override
    public Iterable<Schedule> iterate() {
        return StreamSupport.stream(
            new RtEither(
                new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
            ).permit(this.origin).iterate().spliterator(), false
        ).<Schedule>map(
            schedule -> new SdAuthorized(schedule, this.school, this.actor)
        ).toList();
    }

    @Override
    public void remove(final long id) throws Exception {
        new RtEither(
            new RtAdmin(this.actor), new RtDirectorOrDeputy(this.actor, this.school)
        ).permit(this.origin).remove(id);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return new RtEither(
            new RtAdmin(this.actor), new RtSchoolMember(this.actor, this.school)
        ).permit(this.origin).print(media);
    }
}
