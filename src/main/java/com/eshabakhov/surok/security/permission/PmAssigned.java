/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.user.Roles;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Permission granted by any expected role among current scoped assignments.
 * For example, a director's role applies only within the supplied school.
 *
 * @since 0.0.1
 */
public final class PmAssigned implements Permission {

    /** Lazily read role assignments. */
    private final Iterable<Roles> assignments;

    /** Permitted roles. */
    private final Set<Roles.RoleEnum> expected;

    /**
     * New permission without a school context.
     *
     * @param actor Acting visitor
     * @param expected Permitted roles
     */
    public PmAssigned(final User actor, final Set<Roles.RoleEnum> expected) {
        this(() -> List.of(actor.roles()).iterator(), expected);
    }

    /**
     * New permission with account and target-school roles.
     *
     * @param actor Acting account
     * @param school Target school
     * @param expected Permitted roles
     */
    public PmAssigned(
        final UrAccount actor, final School school, final Set<Roles.RoleEnum> expected
    ) {
        this(
            () -> Stream.concat(
                Stream.of(actor.roles()), Stream.of(school)
                    .filter(target -> target.users().contains(actor))
                    .map(target -> target.users().roles(actor))
            ).iterator(), expected
        );
    }

    /**
     * New permission with deferred assignments.
     *
     * @param assignments Current role sources
     * @param expected Permitted roles
     */
    private PmAssigned(final Iterable<Roles> assignments, final Set<Roles.RoleEnum> expected) {
        this.assignments = assignments;
        this.expected = expected;
    }

    @Override
    public boolean allows() {
        return StreamSupport.stream(this.assignments.spliterator(), false)
            .anyMatch(roles -> this.expected.stream().anyMatch(roles::contains));
    }
}
