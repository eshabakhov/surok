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
import com.eshabakhov.surok.user.Roles;
import java.util.stream.StreamSupport;
import org.springframework.security.access.AccessDeniedException;

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
        if (!this.actor.isAdmin() && (!this.school.users().contains(this.actor)
            || !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DIRECTOR)
            && !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DEPUTY_DIRECTOR))) {
            throw new AccessDeniedException(
                "Only a school director or deputy can create a schedule"
            );
        }
        return new SdAuthorized(this.origin.create(input), this.school, this.actor);
    }

    @Override
    public Schedule schedule(final long id) throws Exception {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view a schedule");
        }
        return new SdAuthorized(this.origin.schedule(id), this.school, this.actor);
    }

    @Override
    public Schedules selection(
        final Filters filters, final Page page, final Sorts sorts
    ) throws Exception {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view schedules");
        }
        return new SdsAuthorized(
            this.origin.selection(filters, page, sorts), this.school, this.actor
        );
    }

    @Override
    public Iterable<Schedule> iterate() {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view schedules");
        }
        return StreamSupport.stream(
            this.origin.iterate().spliterator(), false
        ).<Schedule>map(
            schedule -> new SdAuthorized(schedule, this.school, this.actor)
        ).toList();
    }

    @Override
    public void remove(final long id) throws Exception {
        if (!this.actor.isAdmin() && (!this.school.users().contains(this.actor)
            || !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DIRECTOR)
            && !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DEPUTY_DIRECTOR))) {
            throw new AccessDeniedException(
                "Only a school director or deputy can remove a schedule"
            );
        }
        this.origin.remove(id);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view schedules");
        }
        return this.origin.print(media);
    }
}
