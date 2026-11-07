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
import com.eshabakhov.surok.user.Roles;
import org.springframework.security.access.AccessDeniedException;

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
        if (!this.actor.isAdmin() && (!this.school.users().contains(this.actor)
            || !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DIRECTOR)
            && !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DEPUTY_DIRECTOR))) {
            throw new AccessDeniedException(
                "Only a school director or deputy can revise a schedule"
            );
        }
        return new SdAuthorized(this.origin.revise(input), this.school, this.actor);
    }

    @Override
    public ClassLoads loads() {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view teaching loads");
        }
        return new ClsAuthorized(this.origin.loads(), this.school, this.actor);
    }

    @Override
    public LoadDraft draft() {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view the load draft");
        }
        return new LdAuthorized(this.origin.draft(), this.school, this.actor);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view a schedule");
        }
        return this.origin.print(media);
    }
}
