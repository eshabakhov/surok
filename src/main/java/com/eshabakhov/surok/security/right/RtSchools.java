/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.right;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import java.util.function.Function;
import java.util.stream.StreamSupport;

/**
 * A right available in at least one current school within the inspected scope.
 * For example, Swagger advertises building creation if any school permits the actor to create it.
 *
 * @since 0.0.1
 */
public final class RtSchools implements Right {

    /** Account whose current schools are inspected. */
    private final User actor;

    /** Whether the operation concerns the actor's own schools. */
    private final boolean own;

    /** Right evaluated independently in each school. */
    private final Function<School, Right> rights;

    /**
     * New right across school memberships.
     *
     * @param actor Account to inspect
     * @param own Whether the inspected scope belongs to the account
     * @param rights School-specific right
     */
    public RtSchools(
        final User actor, final boolean own, final Function<School, Right> rights
    ) {
        this.actor = actor;
        this.own = own;
        this.rights = rights;
    }

    @Override
    public boolean allows() {
        return this.own && StreamSupport.stream(
            this.actor.schools().iterate().spliterator(), false
        ).anyMatch(school -> this.rights.apply(school).allows());
    }
}
