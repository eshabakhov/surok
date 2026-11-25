/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Permission granted by at least one bound alternative.
 * For example, a global administrator bypasses the lazy school lookup.
 *
 * @since 0.0.1
 */
public final class PmAnyOf implements Permission {

    /** Alternatives evaluated until access is granted. */
    private final Iterable<Permission> alternatives;

    /**
     * New pair of alternative permissions.
     *
     * @param first First permission
     * @param second Second permission
     */
    public PmAnyOf(final Permission first, final Permission second) {
        this(List.of(first, second));
    }

    /**
     * New disjunction.
     *
     * @param alternatives Bound permissions
     */
    public PmAnyOf(final Iterable<Permission> alternatives) {
        this.alternatives = alternatives;
    }

    @Override
    public boolean allows() {
        return StreamSupport.stream(this.alternatives.spliterator(), false)
            .anyMatch(Permission::allows);
    }
}
