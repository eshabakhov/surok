/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.user.Roles;
import org.springframework.security.access.AccessDeniedException;

/**
 * School subject whose revision follows the actor's current school role.
 * For example, {@code new SbAuthorized(subject, school, actor).revise(input)}
 * refuses a member who lost the school's director role.
 *
 * @since 0.0.1
 */
public final class SbAuthorized implements Subject {

    /** Underlying school subject. */
    private final Subject subject;

    /** Owning school. */
    private final School school;

    /** Acting user. */
    private final User actor;

    /**
     * New protected school subject.
     *
     * @param subject Underlying school subject
     * @param school Owning school
     * @param actor Acting user
     */
    public SbAuthorized(final Subject subject, final School school, final User actor) {
        this.subject = subject;
        this.school = school;
        this.actor = actor;
    }

    @Override
    public Long uid() {
        return this.subject.uid();
    }

    @Override
    public String name() throws Exception {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view a subject");
        }
        return this.subject.name();
    }

    @Override
    public Origin origin() throws Exception {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view a subject");
        }
        return this.subject.origin();
    }

    @Override
    public Subject revise(final SubjectInput input) throws Exception {
        if (!this.actor.isAdmin() && (!this.school.users().contains(this.actor)
            || !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DIRECTOR)
            && !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DEPUTY_DIRECTOR))) {
            throw new AccessDeniedException(
                "Only a school director or deputy can revise a subject"
            );
        }
        return new SbAuthorized(this.subject.revise(input), this.school, this.actor);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view a subject");
        }
        return this.subject.print(media);
    }
}
