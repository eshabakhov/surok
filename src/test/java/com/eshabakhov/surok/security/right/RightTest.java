/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.right;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

/**
 * Verifies that a right returns an object only when access is granted.
 * For example, a permitted domain object can receive its next operation.
 *
 * @since 0.0.1
 */
final class RightTest {

    /**
     * Returns the original object for an allowed operation.
     */
    @Test
    void returnsThePermittedObject() {
        final String origin = "Корпус № 73";
        MatcherAssert.assertThat(
            "The permitted object was not returned",
            ((Right) () -> true).permit(origin),
            Matchers.sameInstance(origin)
        );
    }

    /**
     * Rejects the object before an operation can be invoked.
     */
    @Test
    void rejectsTheForbiddenObject() {
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> ((Right) () -> false).permit("Корпус № 91"),
            "A forbidden object was returned"
        );
    }
}
