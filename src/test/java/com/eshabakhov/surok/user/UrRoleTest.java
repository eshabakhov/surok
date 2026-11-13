/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.User;
import com.eshabakhov.surok.security.right.RtAdmin;
import com.eshabakhov.surok.security.right.RtDirectorOrDeputy;
import com.eshabakhov.surok.security.right.RtEither;
import com.eshabakhov.surok.security.right.RtSchoolDirector;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Representative accounts evaluated by the ordinary domain rights.
 * For example, a deputy can change school resources but cannot revise the school itself.
 *
 * @since 0.0.1
 */
final class UrRoleTest {

    @ParameterizedTest
    @EnumSource(
        value = Roles.RoleEnum.class, names = "ANONYMOUS", mode = EnumSource.Mode.EXCLUDE
    )
    void appliesSchoolModificationRightsToEveryRole(final Roles.RoleEnum role) {
        final User actor = new UrRole(17L, role);
        MatcherAssert.assertThat(
            "A representative account received incorrect school modification rights",
            new RtEither(
                new RtAdmin(actor),
                new RtDirectorOrDeputy(actor, actor.schools().iterate().iterator().next())
            ).allows(),
            Matchers.is(
                List.of(
                    Roles.RoleEnum.ADMIN, Roles.RoleEnum.DIRECTOR, Roles.RoleEnum.DEPUTY_DIRECTOR
                ).contains(role)
            )
        );
    }

    @ParameterizedTest
    @EnumSource(
        value = Roles.RoleEnum.class, names = "ANONYMOUS", mode = EnumSource.Mode.EXCLUDE
    )
    void appliesSchoolRevisionRightsToEveryRole(final Roles.RoleEnum role) {
        final User actor = new UrRole(29L, role);
        MatcherAssert.assertThat(
            "A representative account received incorrect school revision rights",
            new RtEither(
                new RtAdmin(actor),
                new RtSchoolDirector(actor, actor.schools().iterate().iterator().next())
            ).allows(),
            Matchers.is(List.of(Roles.RoleEnum.ADMIN, Roles.RoleEnum.DIRECTOR).contains(role))
        );
    }

    @Test
    void rejectsAnUnrelatedSchoolUser() {
        MatcherAssert.assertThat(
            "A representative school exposed its director role to an unrelated user",
            new RtSchoolDirector(
                new UrRole(43L, Roles.RoleEnum.VIEWER),
                new UrRole(31L, Roles.RoleEnum.DIRECTOR).schools().iterate().iterator().next()
            ).allows(),
            Matchers.is(false)
        );
    }

    @Test
    void recognizesTheSameAccountThroughItsAuthenticationAdapter() {
        final User actor = new UrRole(53L, Roles.RoleEnum.DEPUTY_DIRECTOR);
        MatcherAssert.assertThat(
            "A representative school lost membership through the authentication adapter",
            new RtDirectorOrDeputy(
                new AuthUser(actor), actor.schools().iterate().iterator().next()
            ).allows(),
            Matchers.is(true)
        );
    }
}
