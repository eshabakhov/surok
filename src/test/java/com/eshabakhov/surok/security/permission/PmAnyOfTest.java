/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Any bound permission can grant an operation without evaluating later alternatives.
 * For example, an administrator can act without a school director role.
 *
 * @since 0.0.1
 */
final class PmAnyOfTest {

    /** Stops evaluation after the first permitted alternative. */
    @Test
    void allowsTheFirstPermissionWithoutEvaluatingTheSecond() {
        MatcherAssert.assertThat(
            "The first permission did not permit an operation",
            new PmAnyOf(
                () -> true, () -> {
                    throw new IllegalStateException("The second permission was evaluated");
                }
            ).allows(), Matchers.is(true)
        );
    }

    /** Evaluates later alternatives after a denial. */
    @Test
    void allowsTheSecondPermission() {
        MatcherAssert.assertThat(
            "The second permission did not permit an operation",
            new PmAnyOf(() -> false, () -> true).allows(), Matchers.is(true)
        );
    }

    /** Denies an operation when both alternatives refuse it. */
    @Test
    void deniesBothRefusedPermissions() {
        MatcherAssert.assertThat(
            "An operation was permitted without permission",
            new PmAnyOf(() -> false, () -> false).allows(), Matchers.is(false)
        );
    }

    /** An empty set of alternatives cannot grant access. */
    @Test
    void deniesAnEmptyPermissionCollection() {
        MatcherAssert.assertThat(
            "An empty permission collection granted access",
            new PmAnyOf(List.of()).allows(), Matchers.is(false)
        );
    }
}
