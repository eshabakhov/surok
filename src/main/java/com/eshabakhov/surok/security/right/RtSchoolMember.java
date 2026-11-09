/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.right;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;

/**
 * Right of an administrator or a current school member.
 * For example, a member may inspect buildings in their school.
 *
 * @since 0.0.1
 */
public final class RtSchoolMember implements Right {

    /** Acting user. */
    private final User actor;

    /** School whose membership is checked. */
    private final School school;

    /**
     * New school membership right.
     *
     * @param actor Acting user
     * @param school School to inspect
     */
    public RtSchoolMember(final User actor, final School school) {
        this.actor = actor;
        this.school = school;
    }

    @Override
    public boolean allows() {
        return this.actor.isAdmin() || this.school.users().contains(this.actor);
    }
}
