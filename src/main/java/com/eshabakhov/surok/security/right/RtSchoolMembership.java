/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.right;

import com.eshabakhov.surok.User;

/**
 * Right to address a school by its identifier before loading it.
 * For example, a current member may open its school details.
 *
 * @since 0.0.1
 */
public final class RtSchoolMembership implements Right {

    /** Acting user. */
    private final User actor;

    /** Requested school identifier. */
    private final long school;

    /**
     * New school lookup right.
     *
     * @param actor Acting user
     * @param school Requested school identifier
     */
    public RtSchoolMembership(final User actor, final long school) {
        this.actor = actor;
        this.school = school;
    }

    @Override
    public boolean allows() {
        return this.actor.schools().contains(this.school);
    }
}
