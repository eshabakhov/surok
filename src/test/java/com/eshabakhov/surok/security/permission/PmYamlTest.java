/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.security.OaYaml;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrAcRole;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * The catalog rejects an incomplete authorization contract.
 * For example, a misspelled permission cannot silently expose a Buildings operation.
 *
 * @since 0.0.1
 */
final class PmYamlTest {

    /**
     * Finds a declared right independently of controller mappings and documentation tags.
     *
     * @throws Exception If the specification cannot be read
     */
    @Test
    void resolvesTheDeclaredRightWithoutAController() throws Exception {
        final PmYaml catalog = new PmYaml(
            new OaYaml(
                new ByteArrayResource(
                    """
                    paths:
                        /школы/{school}/корпуса:
                            get:
                                tags: [School premises]
                                operationId: Buildings.create
                                x-permission: {grants: [{schoolRoles: [VIEWER]}]}
                    """.getBytes(StandardCharsets.UTF_8)
                )
            )
        );
        final UrAcRole viewer = new UrAcRole(37L, Roles.RoleEnum.VIEWER);
        MatcherAssert.assertThat(
            "Domain access and documentation did not honor the same custom grant",
            List.of(
                catalog.forOperation("Buildings.create")
                    .right(viewer, viewer.schools().school(37L)).allows(),
                new PsSpring(
                    new PmCatalog(new RequestMappingHandlerMapping(), List.of(), catalog),
                    Locale.ROOT
                ).selection(viewer).allows("/школы/{school}/корпуса", RequestMethod.GET)
            ),
            Matchers.contains(true, true)
        );
    }

    /**
     * Resolves a form declared before its domain operation.
     *
     * @throws Exception If the fixture cannot be read
     */
    @Test
    void resolvesForwardReferences() throws Exception {
        final PmYaml catalog = new PmYaml(
            new OaYaml(
                new ByteArrayResource(
                    """
                    paths:
                        /schools/{school}/buildings/{building}/edit:
                            get:
                                operationId: openBuildingEditForm
                                x-permission: {sameAs: Buildings.building}
                        /schools/{school}/buildings/{building}:
                            get:
                                operationId: Buildings.building
                                x-permission: {grants: [{schoolMember: true}]}
                    """.getBytes(StandardCharsets.UTF_8)
                )
            )
        );
        MatcherAssert.assertThat(
            "Reordering paths changed the form permission",
            catalog.forOperation("openBuildingEditForm"),
            Matchers.sameInstance(catalog.forOperation("Buildings.building"))
        );
    }

    @Test
    void sharesTheBuildingReadPermissionWithTheEditForm() {
        MatcherAssert.assertThat(
            "The edit form must use the domain lookup grants",
            PmYaml.PACKAGED.forOperation("openBuildingEditForm"),
            Matchers.sameInstance(PmYaml.PACKAGED.forOperation("Buildings.building"))
        );
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "", "x-permission: {sameAs: Missing.rule}", "x-permission: {grants: bogus}",
            "x-permission: {sameAs: Buildings.create}", "x-permission: {}"
        }
    )
    void rejectsMissingOrUnknownPermissions(final String declaration) {
        MatcherAssert.assertThat(
            "An invalid declaration must identify the offending building operation",
            Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new PmYaml(
                    new OaYaml(
                        new ByteArrayResource(
                            String.format(
                                """
                                paths:
                                    /schools/{school}/buildings:
                                        post:
                                            tags: [Buildings]
                                            operationId: Buildings.create
                                            %s
                                """,
                                declaration
                            ).getBytes(StandardCharsets.UTF_8)
                        )
                    )
                )
            ).getMessage(),
            Matchers.containsString("Buildings")
        );
    }
}
