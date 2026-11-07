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
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrsAuthorized;
import org.springframework.security.access.AccessDeniedException;

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
        if (!this.actor.isAdmin() && (!this.origin.users().contains(this.actor)
            || !this.origin.users().roles(this.actor).contains(Roles.RoleEnum.DIRECTOR))) {
            throw new AccessDeniedException("Only a school director can revise this school");
        }
        return new SlAuthorized(this.origin.revise(input), this.actor);
    }

    @Override
    public Users users() {
        if (!this.actor.isAdmin() && !this.origin.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view this school");
        }
        return new UrsAuthorized(this.origin.users(), this.actor);
    }

    @Override
    public Buildings buildings() {
        if (!this.actor.isAdmin() && !this.origin.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view this school");
        }
        return new BdsAuthorized(this.origin.buildings(), this.origin, this.actor);
    }

    @Override
    public Teachers teachers() {
        if (!this.actor.isAdmin() && !this.origin.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view this school");
        }
        return new ThsAuthorized(this.origin.teachers(), this.origin, this.actor);
    }

    @Override
    public SchoolClasses schoolClasses() {
        if (!this.actor.isAdmin() && !this.origin.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view this school");
        }
        return new ScsAuthorized(this.origin.schoolClasses(), this.origin, this.actor);
    }

    @Override
    public Subjects subjects() {
        if (!this.actor.isAdmin() && !this.origin.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view this school");
        }
        return new SbsAuthorized(this.origin.subjects(), this.origin, this.actor);
    }

    @Override
    public Schedules schedules() {
        if (!this.actor.isAdmin() && !this.origin.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view this school");
        }
        return new SdsAuthorized(this.origin.schedules(), this.origin, this.actor);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        if (!this.actor.isAdmin() && !this.origin.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view this school");
        }
        return this.origin.print(media);
    }
}
