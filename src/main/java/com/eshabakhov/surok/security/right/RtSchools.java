/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.right;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.Schools;
import java.util.function.Function;
import java.util.stream.StreamSupport;

/**
 * A right available in at least one of the supplied schools.
 * For example, Swagger advertises building creation if any school permits the actor to create it.
 *
 * @since 0.0.1
 */
public final class RtSchools implements Right {

    /** Schools in which the right is evaluated. */
    private final Schools schools;

    /** Right evaluated independently in each school. */
    private final Function<School, Right> rights;

    /**
     * New right across the supplied schools.
     *
     * @param schools Schools to inspect
     * @param rights School-specific right
     */
    public RtSchools(final Schools schools, final Function<School, Right> rights) {
        this.schools = schools;
        this.rights = rights;
    }

    @Override
    public boolean allows() {
        return StreamSupport.stream(
            this.schools.iterate().spliterator(), false
        ).anyMatch(school -> this.rights.apply(school).allows());
    }
}
