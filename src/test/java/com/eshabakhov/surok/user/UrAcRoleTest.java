/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.security.OaYaml;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.access.AccessDeniedException;

/**
 * Representative accounts evaluated by the ordinary domain permissions.
 * For example, a deputy can change school resources but cannot revise the school itself.
 *
 * @since 0.0.1
 */
final class UrAcRoleTest {

    /**
     * Permits global administration through the account contract.
     *
     * @throws Exception If the operation catalog cannot be read
     */
    @Test
    void permitsAGlobalOperationForTheAdministratorRole() throws Exception {
        final UrAccount actor = new UrAcRole(Roles.RoleEnum.ADMIN);
        MatcherAssert.assertThat(
            "The representative administrator lost its permitted account",
            actor.permit("Curriculums.create"), Matchers.sameInstance(actor)
        );
    }

    /**
     * A representative school role applies to its own school only.
     *
     * @throws Exception If the school cannot be read
     */
    @Test
    void permitsTheDirectorRoleWithinItsSchool() throws Exception {
        final UrAccount actor = new UrAcRole(67L, Roles.RoleEnum.DIRECTOR);
        MatcherAssert.assertThat(
            "The representative director could not access its own school",
            actor.permit("Buildings.create", actor.schools().school(67L)),
            Matchers.sameInstance(actor)
        );
    }

    /** Does not grant a school role outside its own school. */
    @Test
    void deniesTheDirectorRoleInAnotherSchool() {
        final UrAccount actor = new UrAcRole(71L, Roles.RoleEnum.DIRECTOR);
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> actor.permit(
                "Buildings.create", new UrAcRole(73L, Roles.RoleEnum.DIRECTOR).schools().school(73L)
            ),
            "The representative director gained permission in another school"
        );
    }

    @ParameterizedTest
    @EnumSource(
        value = Roles.RoleEnum.class, names = "ANONYMOUS", mode = EnumSource.Mode.EXCLUDE
    )
    void appliesSchoolModificationRightsToEveryRole(final Roles.RoleEnum role) throws Exception {
        final UrAccount actor = new UrAcRole(17L, role);
        MatcherAssert.assertThat(
            "A representative account received incorrect school modification rights",
            new OaYaml().operations().operation("Cabinets.create")
                .permission(actor, actor.schools()).allows(),
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
    void appliesSchoolRevisionRightsToEveryRole(final Roles.RoleEnum role) throws Exception {
        final UrAccount actor = new UrAcRole(29L, role);
        MatcherAssert.assertThat(
            "A representative account received incorrect school revision rights",
            new OaYaml().operations().operation("Schools.revise").permission(
                actor, actor.schools()
            ).allows(),
            Matchers.is(List.of(Roles.RoleEnum.ADMIN, Roles.RoleEnum.DIRECTOR).contains(role))
        );
    }

    @Test
    void rejectsAnUnrelatedSchoolUser() throws Exception {
        MatcherAssert.assertThat(
            "A representative school exposed its director role to an unrelated user",
            new OaYaml().operations().operation("Schools.revise").permission(
                new UrAcRole(43L, Roles.RoleEnum.STUDENT),
                new UrAcRole(31L, Roles.RoleEnum.DIRECTOR).schools().iterate().iterator().next()
            ).allows(),
            Matchers.is(false)
        );
    }

    @Test
    void excludesOtherAccountsSchoolsFromItsMemberships() throws Exception {
        final UrAccount actor = new UrAcRole(
            ThreadLocalRandom.current().nextLong(1L, Long.MAX_VALUE - 1L), Roles.RoleEnum.DIRECTOR
        );
        MatcherAssert.assertThat(
            "A representative account belonged to another account's school",
            new OaYaml().operations().operation("Schools.school").permission(
                actor,
                new UrAcRole(actor.uid() + 1L, Roles.RoleEnum.DIRECTOR)
                    .schools().iterate().iterator().next().uid()
            ).allows(),
            Matchers.is(false)
        );
    }

    @Test
    void recognizesTheSameAccountThroughItsAuthenticationAdapter() throws Exception {
        final UrAccount actor = new UrAcRole(53L, Roles.RoleEnum.DEPUTY_DIRECTOR);
        MatcherAssert.assertThat(
            "A representative school lost membership through the authentication adapter",
            new OaYaml().operations().operation("Cabinets.create").permission(
                new UrAcAuth(actor), actor.schools().iterate().iterator().next()
            ).allows(),
            Matchers.is(true)
        );
    }
}
