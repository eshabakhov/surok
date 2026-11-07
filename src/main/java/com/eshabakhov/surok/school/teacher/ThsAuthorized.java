/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.teacher;

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
 * School teachers protected by the acting user's current school role.
 * For example, a director can add a teacher and a viewer can list teachers.
 *
 * @since 0.0.1
 */
public final class ThsAuthorized implements Teachers {

    /** Underlying teachers. */
    private final Teachers origin;

    /** School that owns the teachers. */
    private final School school;

    /** Acting user. */
    private final User actor;

    /**
     * New protected school teachers.
     *
     * @param origin Underlying teachers
     * @param school School that owns them
     * @param actor Acting user
     */
    public ThsAuthorized(final Teachers origin, final School school, final User actor) {
        this.origin = origin;
        this.school = school;
        this.actor = actor;
    }

    @Override
    public Teacher create(final TeacherInput input) throws Exception {
        if (!this.actor.isAdmin() && (!this.school.users().contains(this.actor)
            || !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DIRECTOR)
            && !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DEPUTY_DIRECTOR))) {
            throw new AccessDeniedException(
                "Only a school director or deputy can create a teacher"
            );
        }
        return new ThAuthorized(this.origin.create(input), this.school, this.actor);
    }

    @Override
    public Teacher teacher(final long id) throws Exception {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view a teacher");
        }
        return new ThAuthorized(this.origin.teacher(id), this.school, this.actor);
    }

    @Override
    public Teachers selection(
        final Filters filters, final Page page, final Sorts sorts
    ) throws Exception {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view teachers");
        }
        return new ThsAuthorized(
            this.origin.selection(filters, page, sorts), this.school, this.actor
        );
    }

    @Override
    public Iterable<Teacher> iterate() {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view teachers");
        }
        return StreamSupport.stream(
            this.origin.iterate().spliterator(), false
        ).<Teacher>map(
            teacher -> new ThAuthorized(teacher, this.school, this.actor)
        ).toList();
    }

    @Override
    public void remove(final long id) throws Exception {
        if (!this.actor.isAdmin() && (!this.school.users().contains(this.actor)
            || !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DIRECTOR)
            && !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DEPUTY_DIRECTOR))) {
            throw new AccessDeniedException(
                "Only a school director or deputy can remove a teacher"
            );
        }
        this.origin.remove(id);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view teachers");
        }
        return this.origin.print(media);
    }
}
