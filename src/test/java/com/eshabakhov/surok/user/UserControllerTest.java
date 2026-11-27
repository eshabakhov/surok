/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.school.SlsPostgres;
import com.eshabakhov.surok.school.input.SlInput;
import com.eshabakhov.surok.school.input.SlInputName;
import com.eshabakhov.surok.security.OaYaml;
import com.eshabakhov.surok.security.Operations;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * User endpoints authorize each operation using the actor's current roles.
 * For example, a regular user cannot assign a global subscription role.
 *
 * @since 0.0.1
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
final class UserControllerTest {

    /** Isolated database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    /**
     * Creates an account through the canonical collection path.
     *
     * @param mvc HTTP client
     * @param ctx Database context
     * @throws Exception If registration fails
     */
    @Test
    void registersAccountWithoutAnApiPrefix(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        final String name = UUID.randomUUID().toString();
        final MvcResult result = mvc.perform(
            MockMvcRequestBuilders.post("/users").contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON).content(
                    String.format(
                        """
                        {"username":"%s","password":"ValidPassword1!",
                        "email":"%s@example.com"}
                        """,
                        name, name
                    )
                ).with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andReturn();
        final UrAccount created = new UrsPostgres(ctx).identification(name);
        MatcherAssert.assertThat(
            "Registration did not create an account at the canonical user URI",
            List.of(
                String.valueOf(result.getResponse().getStatus()),
                result.getResponse().getHeader("Location"),
                new ObjectMapper().readTree(result.getResponse().getContentAsString())
            ),
            Matchers.contains(
                Matchers.is(String.valueOf(HttpStatus.CREATED.value())),
                Matchers.is(String.format("/users/%d", created.uid())),
                Matchers.is(
                    new ObjectMapper().readTree(
                        String.format(
                            """
                            {"uid":%d,"credentials":{"username":"%s"},
                            "roles":{"list":[{"uid":%d,"name":"BASIC_MAKER"}]},
                            "info":{"corporate":false,"alive":true},"admin":false}
                            """,
                            created.uid(), name, created.roles().list().getFirst().uid()
                        )
                    )
                )
            )
        );
    }

    /**
     * Rejects a regular user's attempt to list global roles.
     *
     * @param mvc HTTP client
     * @param ctx Database context
     * @throws Exception If request or registration fails
     */
    @Test
    void rejectsRoleListingByARegularUser(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        final UrAccount actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        MatcherAssert.assertThat(
            "A regular user listed global roles",
            mvc.perform(
                MockMvcRequestBuilders.get("/users/{user}/roles", actor.uid()).with(
                    SecurityMockMvcRequestPostProcessors.user(new UrAcAuth(actor))
                )
            ).andReturn().getResponse().getStatus(),
            Matchers.is(HttpStatus.FORBIDDEN.value())
        );
    }

    /**
     * Rejects a regular user's attempt to assign a global role.
     *
     * @param mvc HTTP client
     * @param ctx Database context
     * @throws Exception If request or registration fails
     */
    @Test
    void rejectsRoleAssignmentByARegularUser(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        final UrAccount actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        MatcherAssert.assertThat(
            "A regular user assigned a global role",
            mvc.perform(
                MockMvcRequestBuilders.post("/users/{user}/roles", actor.uid())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"role\":\"PRO_MAKER\"}")
                    .with(SecurityMockMvcRequestPostProcessors.csrf()).with(
                        SecurityMockMvcRequestPostProcessors.user(new UrAcAuth(actor))
                    )
            ).andReturn().getResponse().getStatus(),
            Matchers.is(HttpStatus.FORBIDDEN.value())
        );
    }

    /**
     * Allows an administrator to assign a global role.
     *
     * @param mvc HTTP client
     * @param ctx Database context
     * @throws Exception If request or registration fails
     */
    @Test
    void assignsGlobalRoleForAnAdministrator(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        final UrAccount actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final UrAccount target = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        actor.roles().grant(Roles.RoleEnum.ADMIN);
        final MvcResult result = mvc.perform(
            MockMvcRequestBuilders.post("/users/{user}/roles", target.uid())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"PRO_MAKER\"}")
                .with(SecurityMockMvcRequestPostProcessors.csrf()).with(
                    SecurityMockMvcRequestPostProcessors.user(new UrAcAuth(actor))
                )
        ).andReturn();
        MatcherAssert.assertThat(
            "An administrator did not create a global role at the canonical URI",
            List.of(
                String.valueOf(result.getResponse().getStatus()),
                result.getResponse().getHeader("Location")
            ),
            Matchers.contains(
                Matchers.is(String.valueOf(HttpStatus.CREATED.value())),
                Matchers.startsWith(String.format("/users/%d/roles/", target.uid()))
            )
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void printsGrantedAndListedRolesWithoutReflectiveSerialization(
        final boolean scoped, @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        final UrAccount actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        actor.roles().grant(Roles.RoleEnum.ADMIN);
        final UrAccount target = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        for (final Role role : target.roles().list()) {
            target.roles().revoke(role.uid());
        }
        final School school = actor.schools().create(
            new SlInputName(new SlInput(), "Школа для проверки ролей")
        );
        school.users().add(target);
        final String path;
        final String name;
        final String type;
        final Roles roles;
        if (scoped) {
            path = String.format("/schools/%d/users/%d/roles", school.uid(), target.uid());
            name = "STUDENT";
            type = "application/vnd.surok.school-role.v1+json";
            roles = school.users().roles(target);
        } else {
            path = String.format("/users/%d/roles", target.uid());
            name = "PRO_MAKER";
            type = MediaType.APPLICATION_JSON_VALUE;
            roles = target.roles();
        }
        final MvcResult granted = mvc.perform(
            MockMvcRequestBuilders.post(path).contentType(type)
                .content(String.format("{\"role\":\"%s\"}", name))
                .with(SecurityMockMvcRequestPostProcessors.csrf()).with(
                    SecurityMockMvcRequestPostProcessors.user(new UrAcAuth(actor))
                )
        ).andReturn();
        final String expected = String.format(
            "{\"uid\":%d,\"name\":\"%s\"}", roles.list().getFirst().uid(), name
        );
        MatcherAssert.assertThat(
            "Role endpoints lost the assignment identifier, name or array representation",
            List.of(
                new ObjectMapper().readTree(granted.getResponse().getContentAsString()),
                new ObjectMapper().readTree(
                    mvc.perform(
                        MockMvcRequestBuilders.get(path).with(
                            SecurityMockMvcRequestPostProcessors.user(new UrAcAuth(actor))
                        )
                    ).andReturn().getResponse().getContentAsString()
                )
            ),
            Matchers.contains(
                new ObjectMapper().readTree(expected),
                new ObjectMapper().readTree(String.format("[%s]", expected))
            )
        );
    }

    /**
     * Configures the isolated database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", UserControllerTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", UserControllerTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", UserControllerTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }

    /**
     * Verifies permission selection at the HTTP boundary.
     *
     * @since 0.0.1
     */
    @Nested
    final class Authorization {

        /**
         * Uses only the permission declared for the requested endpoint.
         *
         * @param operation Operation identifier
         * @param ctx Database context
         * @throws Exception If the operation fails
         */
        @ParameterizedTest
        @ValueSource(
            strings = {
                "SchoolUsers.list", "SchoolUsers.add", "SchoolUsers.attach", "SchoolUsers.remove",
                "SchoolRoles.list", "SchoolRoles.grant", "SchoolRoles.revoke"
            }
        )
        void authorizesOnlyTheRequestedOperation(
            final String operation, @Autowired final DSLContext ctx
        ) throws Exception {
            final UrAccount actor = new UrsPostgres(ctx).register(
                UUID.randomUUID().toString(), "ValidPassword1!",
                String.format("%s@example.org", UUID.randomUUID()), false
            );
            actor.roles().grant(Roles.RoleEnum.ADMIN);
            final UrAccount target = new UrsPostgres(ctx).register(
                UUID.randomUUID().toString(), "ValidPassword1!",
                String.format("%s@example.org", UUID.randomUUID()), false
            );
            final School school = new SlsPostgres(ctx).create(
                new SlInputName(new SlInput(), "Школа для проверки доступа")
            );
            school.users().add(target);
            final long assignment = school.users().roles(target)
                .grant(Roles.RoleEnum.STUDENT).uid();
            final Operations catalog = new OaYaml().operations();
            final List<String> checks = new ArrayList<>(1);
            final UrsHttp controller = new UrsHttp(
                ctx, () -> {
                    checks.add(operation);
                    return List.of(catalog.operation(operation));
                }
            );
            final MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(HttpHeaders.ACCEPT, "application/*+json");
            switch (operation) {
                case "SchoolUsers.list" -> controller.schoolUsers(school.uid(), request, actor);
                case "SchoolUsers.add" ->
                    controller.addSchoolUser(school.uid(), target.uid(), request, actor);
                case "SchoolUsers.attach" -> controller.attachSchoolUser(
                    school.uid(), new ObjectMapper().createObjectNode()
                        .put("login", target.account().username()), request, actor
                );
                case "SchoolUsers.remove" ->
                    controller.removeSchoolUser(school.uid(), target.uid(), actor);
                case "SchoolRoles.list" ->
                    controller.schoolRoles(school.uid(), target.uid(), actor);
                case "SchoolRoles.grant" -> controller.grantSchoolRole(
                    school.uid(), target.uid(),
                    new ObjectMapper().readTree("{\"role\":\"TEACHER\"}"), actor
                );
                case "SchoolRoles.revoke" ->
                    controller.revokeSchoolRole(school.uid(), target.uid(), assignment, actor);
                case null, default -> throw new IllegalArgumentException(operation);
            }
            MatcherAssert.assertThat(
                "The controller required unrelated permissions or repeated authorization",
                checks, Matchers.contains(operation)
            );
        }

        /**
         * Authorizes global account and role operations without nested read permissions.
         *
         * @param operation Operation identifier
         * @param ctx Database context
         * @throws Exception If the operation fails
         */
        @ParameterizedTest
        @ValueSource(
            strings = {"Users.remove", "UserRoles.list", "UserRoles.grant", "UserRoles.revoke"}
        )
        void authorizesOnlyTheRequestedGlobalOperation(
            final String operation, @Autowired final DSLContext ctx
        ) throws Exception {
            final UrAccount actor = new UrsPostgres(ctx).register(
                UUID.randomUUID().toString(), "ValidPassword1!",
                String.format("%s@example.org", UUID.randomUUID()), false
            );
            actor.roles().grant(Roles.RoleEnum.ADMIN);
            final UrAccount target = new UrsPostgres(ctx).register(
                UUID.randomUUID().toString(), "ValidPassword1!",
                String.format("%s@example.org", UUID.randomUUID()), false
            );
            final Operations catalog = new OaYaml().operations();
            final List<String> checks = new ArrayList<>(1);
            final UrsHttp controller = new UrsHttp(
                ctx, () -> {
                    checks.add(operation);
                    return List.of(catalog.operation(operation));
                }
            );
            switch (operation) {
                case "Users.remove" -> controller.remove(target.uid(), actor);
                case "UserRoles.list" -> controller.listRoles(target.uid(), actor);
                case "UserRoles.grant" -> controller.assignRole(
                    target.uid(), new ObjectMapper().readTree("{\"role\":\"PRO_MAKER\"}"), actor
                );
                case "UserRoles.revoke" -> controller.removeRole(
                    target.uid(), target.roles().list().getFirst().uid(), actor
                );
                case null, default -> throw new IllegalArgumentException(operation);
            }
            MatcherAssert.assertThat(
                "The global operation required unrelated permissions or repeated authorization",
                checks, Matchers.contains(operation)
            );
        }

        /**
         * Rejects a new role grant after the caller's assignment or membership is removed.
         *
         * @param change Revoked access
         * @param ctx Database context
         * @param mvc HTTP client
         * @throws Exception If setup or the request fails
         */
        @ParameterizedTest
        @CsvSource({"global", "school", "membership"})
        void deniesRoleChangesAfterAccessIsRevoked(
            final String change, @Autowired final DSLContext ctx, @Autowired final MockMvc mvc
        ) throws Exception {
            final UrAccount actor = new UrsPostgres(ctx).register(
                UUID.randomUUID().toString(), "ValidPassword1!",
                String.format("%s@example.org", UUID.randomUUID()), false
            );
            final School school = actor.schools().create(
                new SlInputName(new SlInput(), "Школа после отзыва роли")
            );
            final String path;
            final String type;
            final String name;
            final Roles roles;
            if ("global".equals(change)) {
                actor.roles().grant(Roles.RoleEnum.ADMIN);
                path = String.format("/users/%d/roles", actor.uid());
                type = MediaType.APPLICATION_JSON_VALUE;
                name = "PRO_MAKER";
                roles = actor.roles();
            } else {
                path = String.format("/schools/%d/users/%d/roles", school.uid(), actor.uid());
                type = "application/vnd.surok.school-role.v1+json";
                name = "TEACHER";
                roles = school.users().roles(actor);
            }
            final UrAcAuth principal = new UrAcAuth(actor);
            if ("membership".equals(change)) {
                school.users().remove(actor.uid());
            } else {
                for (final Role role : roles.list()) {
                    roles.revoke(role.uid());
                }
            }
            MatcherAssert.assertThat(
                "A revoked caller granted a role using an existing principal",
                List.of(
                    mvc.perform(
                        MockMvcRequestBuilders.post(path).contentType(type).accept(type)
                            .content(String.format("{\"role\":\"%s\"}", name))
                            .with(SecurityMockMvcRequestPostProcessors.csrf())
                            .with(SecurityMockMvcRequestPostProcessors.user(principal))
                    ).andReturn().getResponse().getStatus(),
                    roles.contains(Roles.RoleEnum.valueOf(name))
                ),
                Matchers.contains(HttpStatus.FORBIDDEN.value(), false)
            );
        }
    }
}
