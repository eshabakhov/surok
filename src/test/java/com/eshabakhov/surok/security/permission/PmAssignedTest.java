/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrAcRole;
import com.eshabakhov.surok.user.UrAnonymous;
import java.util.Set;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * One role list is evaluated against account and target-school assignments.
 * For example, a director in another school cannot use the same local permission.
 *
 * @since 0.0.1
 */
final class PmAssignedTest {

    /**
     * Preserves the scope of each assignment with one expected role list.
     *
     * @param role Account role
     * @param target School identifier
     * @param expected Expected access
     * @throws Exception If the school cannot be resolved
     */
    @ParameterizedTest
    @CsvSource({
        "ADMIN, 83, true", "DIRECTOR, 37, true", "DIRECTOR, 83, false",
        "STUDENT, 37, false", "BASIC_MAKER, 37, false"
    })
    void respectsTheScopeOfAssignedRoles(
        final Roles.RoleEnum role, final long target, final boolean expected
    ) throws Exception {
        MatcherAssert.assertThat(
            "The permission ignored the expected role or its school",
            new PmAssigned(
                new UrAcRole(37L, role), new UrAcRole(target, role).schools().school(target),
                Set.of(Roles.RoleEnum.ADMIN, Roles.RoleEnum.DIRECTOR)
            ).allows(), Matchers.is(expected)
        );
    }

    /** Grants anonymous access only when that role is explicitly listed. */
    @Test
    void permitsAnExplicitAnonymousRole() {
        MatcherAssert.assertThat(
            "The declared anonymous role was denied",
            new PmAssigned(new UrAnonymous(), Set.of(Roles.RoleEnum.ANONYMOUS)).allows(),
            Matchers.is(true)
        );
    }

    /** An empty list grants access to nobody. */
    @Test
    void deniesAnEmptyRoleList() {
        MatcherAssert.assertThat(
            "An empty role list granted administrator access",
            new PmAssigned(new UrAcRole(Roles.RoleEnum.ADMIN), Set.of()).allows(),
            Matchers.is(false)
        );
    }

    /** School roles are unavailable without a target school. */
    @Test
    void deniesSchoolRolesWithoutASchool() {
        MatcherAssert.assertThat(
            "A local student role became a global permission",
            new PmAssigned(new UrAcRole(Roles.RoleEnum.STUDENT), Set.of(Roles.RoleEnum.STUDENT))
                .allows(), Matchers.is(false)
        );
    }
}
