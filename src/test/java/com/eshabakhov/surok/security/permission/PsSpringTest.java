/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.security.OaYaml;
import com.eshabakhov.surok.security.Operation;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrAcRole;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Locale;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Operation permissions filtered for visitors and printed as a role matrix.
 * For example, every operation has one cell per role with its access scope.
 *
 * @since 0.0.1
 */
final class PsSpringTest {

    /**
     * Prints each role once and preserves its scope.
     *
     * @throws Exception If printing fails
     */
    @Test
    void printsEveryRoleWithItsAccessScope() throws Exception {
        final Operation operation = new OaYaml().operations().operation("Buildings.remove");
        final JsonNode access = new PsSpring(
            () -> List.of(operation), Locale.forLanguageTag("ru")
        ).print(new MdJson()).print().path("operations").path(0).path("access");
        MatcherAssert.assertThat(
            "The matrix omitted a role or changed its access scope",
            List.of(access.findValuesAsText("role"), access.findValuesAsText("scope")),
            Matchers.contains(
                List.of(
                    "ADMIN", "DIRECTOR", "DEPUTY_DIRECTOR", "TEACHER", "STUDENT",
                    "BASIC_MAKER", "ADVANCED_MAKER", "PRO_MAKER", "ANONYMOUS"
                ),
                List.of(
                    "all", "school", "school", "denied", "denied",
                    "denied", "denied", "denied", "denied"
                )
            )
        );
    }

    /**
     * Rejects protected operations for anonymous visitors.
     *
     * @throws Exception If the document cannot be read
     */
    @Test
    void deniesAnonymousBuildingCreation() throws Exception {
        MatcherAssert.assertThat(
            "An anonymous visitor received building creation permission",
            new PsSpring(
                new OaYaml().operations(), Locale.ROOT
            ).allows("/schools/{school}/buildings", RequestMethod.POST),
            Matchers.is(false)
        );
    }

    /**
     * Uses the operation's declared roles for the selected account.
     *
     * @param role Acting role
     * @param expected Expected access
     * @throws Exception If the document cannot be read
     */
    @ParameterizedTest
    @CsvSource({
        "ADMIN, true", "DIRECTOR, true", "DEPUTY_DIRECTOR, true", "TEACHER, false",
        "STUDENT, false", "BASIC_MAKER, false", "ADVANCED_MAKER, false", "PRO_MAKER, false"
    })
    void honorsTheSelectedAccountsRole(final Roles.RoleEnum role, final boolean expected)
        throws Exception {
        MatcherAssert.assertThat(
            "The selected audience received incorrect building creation permission",
            new PsSpring(
                new OaYaml().operations(),
                Locale.forLanguageTag("ru")
            ).selection(new UrAcRole(37L, role))
                .allows("/schools/{school}/buildings", RequestMethod.POST),
            Matchers.is(expected)
        );
    }
}
