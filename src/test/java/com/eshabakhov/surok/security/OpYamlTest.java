/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import com.eshabakhov.surok.Schools;
import com.eshabakhov.surok.school.SlsFake;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrAcRole;
import com.eshabakhov.surok.user.UrAnonymous;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * OpenAPI operations bind roles to a visitor and a target without an intermediate right.
 * For example, a school's student can be granted creation independently of global administrators.
 *
 * @since 0.0.1
 */
final class OpYamlTest {

    /**
     * Separates authentication from role restrictions, including accounts without schools.
     *
     * @param path Requested path
     * @param method HTTP method
     * @param anonymous Anonymous access
     * @throws Exception If the document cannot be read
     */
    @ParameterizedTest
    @CsvSource({
        "/users, POST, true", "/users/login, GET, true",
        "/users/register, GET, true", "/users/profile, GET, false",
        "/users/subscription, GET, false", "/schools, GET, false",
        "/schools, POST, false", "/federal/subjects, GET, false"
    })
    void separatesAuthenticationFromRoleRequirements(
        final String path, final RequestMethod method, final boolean anonymous
    ) throws Exception {
        final Operation operation = new OaYaml()
            .operations().operation(path, method);
        MatcherAssert.assertThat(
            "Authentication requirements excluded an account or admitted an anonymous visitor",
            List.of(
                operation.permission(new UrAnonymous()).allows(),
                operation.permission(
                    new UrAcRole(91L, Roles.RoleEnum.BASIC_MAKER), new SlsFake(List.of())
                ).allows()
            ), Matchers.contains(anonymous, true)
        );
    }

    /**
     * Preserves global roles when the account belongs to no school.
     *
     * @throws Exception If the document cannot be read
     */
    @Test
    void permitsAdministratorsWithoutSchools() throws Exception {
        MatcherAssert.assertThat(
            "A global administrator required a school to see building creation",
            new OaYaml().operations()
                .operation("Buildings.create").permission(
                    new UrAcRole(73L, Roles.RoleEnum.ADMIN), new SlsFake(List.of())
                ).allows(), Matchers.is(true)
        );
    }

    /**
     * Denies anonymous visitors without inventing an account for them.
     *
     * @throws Exception If the document cannot be read
     */
    @Test
    void deniesAnonymousBuildingCreation() throws Exception {
        MatcherAssert.assertThat(
            "An anonymous visitor acquired building creation permission",
            new OaYaml().operations()
                .operation("Buildings.create").permission(new UrAnonymous()).allows(),
            Matchers.is(false)
        );
    }

    /**
     * Evaluates the same declarations against one school and a collection.
     *
     * @param role Acting role
     * @param target School identifier
     * @param expected Expected access
     * @throws Exception If the school cannot be found
     */
    @ParameterizedTest
    @CsvSource({
        "STUDENT, 37, true", "STUDENT, 83, false", "ADMIN, 37, false",
        "DIRECTOR, 37, false", "BASIC_MAKER, 83, true"
    })
    void honorsTheDeclaredRoles(
        final Roles.RoleEnum role, final long target, final boolean expected
    ) throws Exception {
        final UrAcRole actor = new UrAcRole(37L, role);
        final Schools schools = new UrAcRole(target, role).schools();
        final Operation operation = new OaYaml(
            new ByteArrayResource(
                """
                paths:
                    /школы/{school}/корпуса:
                        post:
                            operationId: Buildings.create
                            x-roles: [STUDENT, BASIC_MAKER]
                """.getBytes(StandardCharsets.UTF_8)
            )
        ).operations().operation("Buildings.create");
        MatcherAssert.assertThat(
            "The single-school and multi-school grants disagreed with the declaration",
            List.of(
                operation.permission(actor, schools.school(target)).allows(),
                operation.permission(actor, schools).allows()
            ), Matchers.contains(expected, expected)
        );
    }
}
