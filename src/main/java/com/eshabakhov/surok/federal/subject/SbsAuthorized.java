/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

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
 * School subject catalogue protected by the actor's current school role.
 * For example, {@code new SbsAuthorized(subjects, school, actor).create(input)}
 * permits the school's director or deputy to add a local subject.
 *
 * @since 0.0.1
 */
public final class SbsAuthorized implements Subjects {

    /** Underlying school catalogue. */
    private final Subjects origin;

    /** Owning school. */
    private final School school;

    /** Acting user. */
    private final User actor;

    /**
     * New protected school catalogue.
     *
     * @param origin Underlying school catalogue
     * @param school Owning school
     * @param actor Acting user
     */
    public SbsAuthorized(final Subjects origin, final School school, final User actor) {
        this.origin = origin;
        this.school = school;
        this.actor = actor;
    }

    @Override
    public Subject create(final SubjectInput input) throws Exception {
        if (!this.actor.isAdmin() && (!this.school.users().contains(this.actor)
            || !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DIRECTOR)
            && !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DEPUTY_DIRECTOR))) {
            throw new AccessDeniedException(
                "Only a school director or deputy can create a subject"
            );
        }
        return new SbAuthorized(this.origin.create(input), this.school, this.actor);
    }

    @Override
    public Subject subject(final long id) throws Exception {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view a subject");
        }
        return new SbAuthorized(this.origin.subject(id), this.school, this.actor);
    }

    @Override
    public Subject revise(final long id, final SubjectInput input) throws Exception {
        if (!this.actor.isAdmin() && (!this.school.users().contains(this.actor)
            || !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DIRECTOR)
            && !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DEPUTY_DIRECTOR))) {
            throw new AccessDeniedException(
                "Only a school director or deputy can revise a subject"
            );
        }
        return new SbAuthorized(this.origin.revise(id, input), this.school, this.actor);
    }

    @Override
    public Subjects selection(
        final Filters filters, final Page page, final Sorts sorts
    ) throws Exception {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view subjects");
        }
        return new SbsAuthorized(
            this.origin.selection(filters, page, sorts), this.school, this.actor
        );
    }

    @Override
    public Iterable<Subject> iterate() {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view subjects");
        }
        return StreamSupport.stream(this.origin.iterate().spliterator(), false)
            .<Subject>map(subject -> new SbAuthorized(subject, this.school, this.actor)).toList();
    }

    @Override
    public void remove(final long id) throws Exception {
        if (!this.actor.isAdmin() && (!this.school.users().contains(this.actor)
            || !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DIRECTOR)
            && !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DEPUTY_DIRECTOR))) {
            throw new AccessDeniedException(
                "Only a school director or deputy can remove a subject"
            );
        }
        this.origin.remove(id);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view subjects");
        }
        return this.origin.print(media);
    }
}
