/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.autofill;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.federal.curriculum.Curriculum;
import com.eshabakhov.surok.federal.subject.Subject;
import com.eshabakhov.surok.school.schedule.load.hours.WeeklyHours;
import com.eshabakhov.surok.user.Roles;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;

/**
 * Teaching load draft protected by the acting user's current school role.
 * For example, a viewer can inspect a draft while a director can apply it.
 *
 * @since 0.0.1
 */
public final class LdAuthorized implements LoadDraft {

    /** Underlying draft. */
    private final LoadDraft origin;

    /** Owning school. */
    private final School school;

    /** Acting user. */
    private final User actor;

    /**
     * New protected draft.
     *
     * @param origin Underlying draft
     * @param school Owning school
     * @param actor Acting user
     */
    public LdAuthorized(final LoadDraft origin, final School school, final User actor) {
        this.origin = origin;
        this.school = school;
        this.actor = actor;
    }

    @Override
    public void save(final LoadDraftInput input) throws Exception {
        if (!this.actor.isAdmin() && (!this.school.users().contains(this.actor)
            || !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DIRECTOR)
            && !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DEPUTY_DIRECTOR))) {
            throw new AccessDeniedException(
                "Only a school director or deputy can save the load draft"
            );
        }
        this.origin.save(input);
    }

    @Override
    public void save(
        final Curriculum curriculum, final Map<Integer, Map<Subject, WeeklyHours>> additions
    ) throws Exception {
        if (!this.actor.isAdmin() && (!this.school.users().contains(this.actor)
            || !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DIRECTOR)
            && !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DEPUTY_DIRECTOR))) {
            throw new AccessDeniedException(
                "Only a school director or deputy can save the load draft"
            );
        }
        this.origin.save(curriculum, additions);
    }

    @Override
    public void revise(final LoadDraftInput input) throws Exception {
        if (!this.actor.isAdmin() && (!this.school.users().contains(this.actor)
            || !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DIRECTOR)
            && !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DEPUTY_DIRECTOR))) {
            throw new AccessDeniedException(
                "Only a school director or deputy can revise the load draft"
            );
        }
        this.origin.revise(input);
    }

    @Override
    public void revise(final Curriculum curriculum) throws Exception {
        if (!this.actor.isAdmin() && (!this.school.users().contains(this.actor)
            || !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DIRECTOR)
            && !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DEPUTY_DIRECTOR))) {
            throw new AccessDeniedException(
                "Only a school director or deputy can revise the load draft"
            );
        }
        this.origin.revise(curriculum);
    }

    @Override
    public Curriculum curriculum() throws Exception {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view the load draft");
        }
        return this.origin.curriculum();
    }

    @Override
    public void apply() throws Exception {
        if (!this.actor.isAdmin() && (!this.school.users().contains(this.actor)
            || !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DIRECTOR)
            && !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DEPUTY_DIRECTOR))) {
            throw new AccessDeniedException(
                "Only a school director or deputy can apply the load draft"
            );
        }
        this.origin.apply();
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view the load draft");
        }
        return this.origin.print(media);
    }
}
