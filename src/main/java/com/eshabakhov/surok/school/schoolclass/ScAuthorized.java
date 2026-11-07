/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schoolclass;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.user.Roles;
import org.springframework.security.access.AccessDeniedException;

/**
 * School class that checks the acting user's role for each operation.
 * For example, a deputy can revise a class returned by a protected collection.
 *
 * @since 0.0.1
 */
public final class ScAuthorized implements SchoolClass {

    /** Underlying class. */
    private final SchoolClass origin;

    /** School that owns the class. */
    private final School school;

    /** Acting user. */
    private final User actor;

    /**
     * New protected school class.
     *
     * @param origin Underlying class
     * @param school School that owns it
     * @param actor Acting user
     */
    public ScAuthorized(final SchoolClass origin, final School school, final User actor) {
        this.origin = origin;
        this.school = school;
        this.actor = actor;
    }

    @Override
    public Long uid() {
        return this.origin.uid();
    }

    @Override
    public int grade() throws Exception {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view a class");
        }
        return this.origin.grade();
    }

    @Override
    public SchoolClass revise(final SchoolClassInput input) throws Exception {
        if (!this.actor.isAdmin() && (!this.school.users().contains(this.actor)
            || !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DIRECTOR)
            && !this.school.users().roles(this.actor).contains(Roles.RoleEnum.DEPUTY_DIRECTOR))) {
            throw new AccessDeniedException("Only a school director or deputy can revise a class");
        }
        return new ScAuthorized(this.origin.revise(input), this.school, this.actor);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        if (!this.actor.isAdmin() && !this.school.users().contains(this.actor)) {
            throw new AccessDeniedException("Only a school member can view a class");
        }
        return this.origin.print(media);
    }
}
