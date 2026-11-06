/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schoolclass;

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
 * School classes protected by the acting user's current school role.
 * For example, a director can create a class and a viewer can list classes.
 *
 * @since 0.0.1
 */
public final class ScsAuthorized implements SchoolClasses {

    /** Underlying classes. */
    private final SchoolClasses origin;

    /** School that owns the classes. */
    private final School school;

    /** Acting user. */
    private final User actor;

    /**
     * New protected school classes.
     *
     * @param origin Underlying classes
     * @param school School that owns them
     * @param actor Acting user
     */
    public ScsAuthorized(final SchoolClasses origin, final School school, final User actor) {
        this.origin = origin;
        this.school = school;
        this.actor = actor;
    }

    @Override
    public SchoolClass create(final SchoolClassInput input) throws Exception {
        if (!this.actor.isAdmin() && (!this.school.users().contains(this.actor)
            || !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DIRECTOR)
            && !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DEPUTY_DIRECTOR))) {
            throw new AccessDeniedException("Only a school director or deputy can create a class");
        }
        return new ScAuthorized(this.origin.create(input), this.school, this.actor);
    }

    @Override
    public SchoolClass clazz(final long id) throws Exception {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view a class");
        }
        return new ScAuthorized(this.origin.clazz(id), this.school, this.actor);
    }

    @Override
    public SchoolClasses selection(
        final Filters filters, final Page page, final Sorts sorts
    ) throws Exception {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view classes");
        }
        return new ScsAuthorized(
            this.origin.selection(filters, page, sorts), this.school, this.actor
        );
    }

    @Override
    public Iterable<SchoolClass> iterate() {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view classes");
        }
        return StreamSupport.stream(
            this.origin.iterate().spliterator(), false
        ).<SchoolClass>map(
            clazz -> new ScAuthorized(clazz, this.school, this.actor)
        ).toList();
    }

    @Override
    public void remove(final long id) throws Exception {
        if (!this.actor.isAdmin() && (!this.school.users().contains(this.actor)
            || !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DIRECTOR)
            && !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DEPUTY_DIRECTOR))) {
            throw new AccessDeniedException("Only a school director or deputy can remove a class");
        }
        this.origin.remove(id);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view classes");
        }
        return this.origin.print(media);
    }
}
