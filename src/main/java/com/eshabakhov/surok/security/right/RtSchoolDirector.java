/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.right;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.user.Roles;

/**
 * Right of a school director to revise a school.
 * For example, a deputy director cannot change the school's own details.
 *
 * @since 0.0.1
 */
public final class RtSchoolDirector implements Right {

    /** Acting user. */
    private final User actor;

    /** School to revise. */
    private final School school;

    /**
     * New school revision right.
     *
     * @param actor Acting user
     * @param school School to revise
     */
    public RtSchoolDirector(final User actor, final School school) {
        this.actor = actor;
        this.school = school;
    }

    @Override
    public boolean allows() {
        return new RtSchoolMember(this.actor, this.school).allows()
            && this.school.users().roles(this.actor).contains(Roles.RoleEnum.DIRECTOR);
    }
}
