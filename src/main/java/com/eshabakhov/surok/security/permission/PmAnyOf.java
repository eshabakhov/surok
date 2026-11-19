/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.Schools;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.security.right.Right;
import java.util.List;

/**
 * Disjunction of the grants declared for one operation.
 * For example, creation may be granted globally or in one school.
 *
 * @since 0.0.1
 */
final class PmAnyOf implements Permission {

    /** Immutable alternatives. */
    private final List<Permission> alternatives;

    /**
     * New disjunction.
     *
     * @param alternatives Declared grants
     */
    PmAnyOf(final List<Permission> alternatives) {
        this.alternatives = alternatives;
    }

    @Override
    public Right right() {
        return () -> this.alternatives.stream().anyMatch(rule -> rule.right().allows());
    }

    @Override
    public Right right(final UrAccount actor, final School school) {
        return () -> this.alternatives.stream().anyMatch(
            rule -> rule.right(actor, school).allows()
        );
    }

    @Override
    public Right right(final UrAccount actor, final Schools schools) {
        return () -> this.alternatives.stream().anyMatch(
            rule -> rule.right(actor, schools).allows()
        );
    }
}
