/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.right;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.user.Roles;

/**
 * Right of a school director or deputy director.
 * For example, a deputy director may create a building in their school.
 *
 * @since 0.0.1
 */
public final class RtDirectorOrDeputy implements Right {

    /** Acting user. */
    private final User actor;

    /** School whose buildings may be changed. */
    private final School school;

    /**
     * New building modification right.
     *
     * @param actor Acting user
     * @param school School to inspect
     */
    public RtDirectorOrDeputy(final User actor, final School school) {
        this.actor = actor;
        this.school = school;
    }

    @Override
    public boolean allows() {
        return new RtSchoolMember(this.actor, this.school).allows()
            && (this.school.users().roles(this.actor).contains(Roles.RoleEnum.DIRECTOR)
            || this.school.users().roles(this.actor).contains(Roles.RoleEnum.DEPUTY_DIRECTOR));
    }
}
