/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.User;
import org.springframework.security.access.AccessDeniedException;

/**
 * Federal requirement whose revision requires an administrator.
 * For example, {@code new RqAuthorized(requirement, user).revise(input)}
 * checks the user's current global role before changing the requirement.
 *
 * @since 0.0.1
 */
public final class RqAuthorized implements Requirement {

    /** Underlying requirement. */
    private final Requirement origin;

    /** Acting user. */
    private final User actor;

    /**
     * New protected requirement.
     *
     * @param origin Underlying requirement
     * @param actor Acting user
     */
    public RqAuthorized(final Requirement origin, final User actor) {
        this.origin = origin;
        this.actor = actor;
    }

    @Override
    public Long uid() {
        return this.origin.uid();
    }

    @Override
    public Requirement revise(final RequirementInput input) throws Exception {
        if (!this.actor.isAdmin()) {
            throw new AccessDeniedException("Only an administrator can revise a requirement");
        }
        return new RqAuthorized(this.origin.revise(input), this.actor);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.origin.print(media);
    }
}
