/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.Schools;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.school.SlsFake;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrAcRole;
import java.util.List;
import java.util.Locale;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Permission decoration preserves the underlying authorization rule.
 * For example, adding matrix columns cannot grant a viewer building removal access.
 *
 * @since 0.0.1
 */
final class PmRolesTest {

    @Test
    void printsEveryRoleExactlyOnce() throws Exception {
        MatcherAssert.assertThat(
            "The matrix omitted or duplicated a role column",
            new PmRoles<>(
                new PmBuilding(
                    "/schools/{school}/buildings/{building}", "Buildings.remove",
                    PmYaml.PACKAGED.forOperation("Buildings.remove"), ""
                ),
                RequestMethod.DELETE, Locale.forLanguageTag("ru")
            ).print(new MdJson()).print().path("access").findValuesAsText("role"),
            Matchers.contains(
                "ADMIN", "DIRECTOR", "DEPUTY_DIRECTOR", "TEACHER", "STUDENT",
                "BASIC_MAKER", "ADVANCED_MAKER", "PRO_MAKER", "VIEWER", "ANONYMOUS"
            )
        );
    }

    @Test
    void preservesGlobalAccessWithoutSchools() {
        MatcherAssert.assertThat(
            "The operation decorator required a school for a global grant",
            new PmRoles<>(
                new PmBuilding(
                    "/schools/{school}/buildings", "Buildings.create",
                    PmYaml.PACKAGED.forOperation("Buildings.create"), ""
                ),
                RequestMethod.POST, Locale.ROOT
            ).right(new UrAcRole(37L, Roles.RoleEnum.ADMIN), new SlsFake(List.of())).allows(),
            Matchers.is(true)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "DIRECTOR, 17, true", "DIRECTOR, 29, false", "DEPUTY_DIRECTOR, 17, true",
        "DEPUTY_DIRECTOR, 29, false", "VIEWER, 17, false", "VIEWER, 29, false",
        "ADMIN, 17, true", "ADMIN, 29, true"
    })
    void preservesTheOriginalRuleForEachSchoolScope(
        final Roles.RoleEnum role, final long member, final boolean expected
    ) throws Exception {
        final Permission operation = new PmRoles<>(
            new PmBuilding(
                "/schools/{school}/buildings/{building}", "Buildings.remove",
                PmYaml.PACKAGED.forOperation("Buildings.remove"), ""
            ),
            RequestMethod.DELETE, Locale.forLanguageTag("ru")
        );
        final UrAcRole actor = new UrAcRole(17L, role);
        final Schools schools = new UrAcRole(member, role).schools();
        MatcherAssert.assertThat(
            "The decorated permission changed the scope of the domain grant",
            List.of(
                operation.right(actor, schools.school(member)).allows(),
                operation.right(actor, schools).allows(), operation.right().allows()
            ),
            Matchers.contains(expected, expected, false)
        );
    }
}
