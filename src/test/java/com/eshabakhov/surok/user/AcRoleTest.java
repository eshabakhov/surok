/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Representative account state used to evaluate role permissions.
 * For example, a representative director has no real password or persisted account data.
 *
 * @since 0.0.1
 */
final class AcRoleTest {

    @ParameterizedTest
    @EnumSource(Roles.RoleEnum.class)
    void distinguishesAnonymousAccessFromIdentifiedRoles(final Roles.RoleEnum role) {
        MatcherAssert.assertThat(
            "A representative account received an incorrect authentication marker",
            new AcRole(role).authenticated(), Matchers.is(role != Roles.RoleEnum.ANONYMOUS)
        );
    }

    @Test
    void refusesToInventLoginCredentials() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new AcRole(Roles.RoleEnum.DIRECTOR).username(),
            "A representative account exposed invented login credentials"
        );
    }

    @Test
    void refusesToInventAPassword() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new AcRole(Roles.RoleEnum.TEACHER).password(),
            "A representative account exposed an invented password"
        );
    }
}
