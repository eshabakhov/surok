/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.Schools;
import com.eshabakhov.surok.school.SlsFake;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrAcRole;
import io.swagger.v3.core.util.Yaml;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Building rights follow the declared grants for both individual and multiple schools.
 * For example, a viewer may create a building only when the catalog grants that operation.
 *
 * @since 0.0.1
 */
final class PmGrantsTest {

    @Test
    void preservesGlobalAccessWithoutSchoolMembership() {
        MatcherAssert.assertThat(
            "The composite required a school for a global administrator",
            PmYaml.PACKAGED.forOperation("Buildings.create").right(
                new UrAcRole(73L, Roles.RoleEnum.ADMIN), new SlsFake(List.of())
            ).allows(),
            Matchers.is(true)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "VIEWER, 37, true", "VIEWER, 83, false", "ADMIN, 37, false",
        "DIRECTOR, 37, false", "BASIC_MAKER, 83, true"
    })
    void evaluatesTheConfiguredRolesInTheSuppliedSchools(
        final Roles.RoleEnum role, final long target, final boolean expected
    ) throws Exception {
        final UrAcRole actor = new UrAcRole(37L, role);
        final Schools schools = new UrAcRole(target, role).schools();
        final Permission grant = new PmGrants(
            Yaml.mapper().readTree(
                """
                /schools/{school}/buildings:
                    post:
                        operationId: Buildings.create
                        x-permission:
                            grants:
                                - schoolRoles: [VIEWER]
                                - globalRoles: [BASIC_MAKER]
                """
            )
        ).forOperation("Buildings.create");
        MatcherAssert.assertThat(
            "The API and catalog did not both follow the configured Buildings grant",
            List.of(
                grant.right(actor, schools.school(target)).allows(),
                grant.right(actor, schools).allows()
            ),
            Matchers.contains(expected, expected)
        );
    }
}
