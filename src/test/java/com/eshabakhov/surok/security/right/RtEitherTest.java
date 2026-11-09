/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.right;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Verifies that either of two independent rights may permit an operation.
 * For example, an administrator can act without a school director role.
 *
 * @since 0.0.1
 */
final class RtEitherTest {

    /**
     * Allows the first right without consulting the second one.
     */
    @Test
    void allowsTheFirstRightWithoutEvaluatingTheSecond() {
        MatcherAssert.assertThat(
            "The first right did not permit an operation",
            new RtEither(
                () -> true, () -> {
                    throw new IllegalStateException("The second right was evaluated");
                }
            ).allows(), Matchers.is(true)
        );
    }

    /**
     * Allows an operation when the second right permits it.
     */
    @Test
    void allowsTheSecondRight() {
        MatcherAssert.assertThat(
            "The second right did not permit an operation",
            new RtEither(() -> false, () -> true).allows(), Matchers.is(true)
        );
    }

    /**
     * Denies an operation when both rights refuse it.
     */
    @Test
    void deniesBothRefusedRights() {
        MatcherAssert.assertThat(
            "An operation was permitted without a right",
            new RtEither(() -> false, () -> false).allows(), Matchers.is(false)
        );
    }
}
