/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.user.Roles;
import java.util.Locale;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Access scopes evaluated from bound rights without constructing an account.
 * For example, an operation allowed only in the caller's school has school scope.
 *
 * @since 0.0.1
 */
final class GrRoleTest {

    @ParameterizedTest
    @CsvSource({"true, true, ALL", "true, false, SCHOOL", "false, false, DENIED"})
    void evaluatesTheScopeOfTheBoundRights(
        final boolean own, final boolean external, final Grant.Scope expected
    ) {
        MatcherAssert.assertThat(
            "The grant received an incorrect access scope",
            new GrRole(
                () -> own, () -> external, Roles.RoleEnum.DIRECTOR, Locale.forLanguageTag("ru")
            ).scope(), Matchers.is(expected)
        );
    }
}
