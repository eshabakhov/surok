/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.security.Operations;

/**
 * Federal requirement whose revision requires an administrator.
 * For example, {@code new RqAuthorized(requirement, user, operations).revise(input)}
 * checks the user's current global role before changing the requirement.
 *
 * @since 0.0.1
 */
public final class RqAuthorized implements Requirement {

    /** Shared operation declarations. */
    private final Operations operations;

    /** Underlying requirement. */
    private final Requirement origin;

    /** Acting user. */
    private final UrAccount actor;

    /**
     * New protected object with shared operation declarations.
     *
     * @param origin Underlying origin
     * @param actor Acting user
     * @param operations Shared operation declarations
     */
    public RqAuthorized(
        final Requirement origin, final UrAccount actor,
        final Operations operations
    ) {
        this.origin = origin;
        this.actor = actor;
        this.operations = operations;
    }

    @Override
    public Long uid() {
        return this.origin.uid();
    }

    @Override
    public Requirement revise(final RequirementInput input) throws Exception {
        return new RqAuthorized(
            this.operations.operation("Requirements.revise")
                .permission(this.actor)
                .permit(this.origin).revise(input), this.actor, this.operations
        );
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.operations.operation("Requirements.requirement")
            .permission(this.actor)
            .permit(this.origin).print(media);
    }
}
