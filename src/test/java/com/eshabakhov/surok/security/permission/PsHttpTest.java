/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrAcAuth;
import com.eshabakhov.surok.user.UrAcPostgres;
import com.eshabakhov.surok.user.UrsPostgres;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.StreamSupport;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Permission matrix, Swagger visibility and HTTP access against the same database.
 * For example, a director sees an operation in Swagger but cannot change another school.
 *
 * @since 0.0.1
 */
@Testcontainers
@DirtiesContext
@SpringBootTest(properties = "app.security.docs.enabled=true")
@AutoConfigureMockMvc
@Sql(
    statements = {
        "TRUNCATE TABLE public.user RESTART IDENTITY CASCADE",
        "TRUNCATE TABLE school, subject, federal_curriculum RESTART IDENTITY CASCADE",
        "INSERT INTO school (id, name) VALUES (1, 'Лицей'), (2, 'Гимназия')",
        """
        INSERT INTO building (id, school_id, name)
        VALUES (10, 1, 'Учебный корпус'), (20, 2, 'Корпус гимназии');
        """,
        "INSERT INTO cabinet (id, building_id, name) VALUES (30, 10, 'Аудитория')",
        "INSERT INTO schedule (id, school_id, name) VALUES (101, 1, 'Учебный год')",
        "INSERT INTO school_class (id, school_id, grade, litera) VALUES (17, 1, 5, 'А')",
        """
        INSERT INTO subject (id, school_id, origin, name)
        VALUES (29, NULL, 'FEDERAL', 'Математика'), (31, 1, 'SCHOOL', 'Робототехника');
        """,
        """
        INSERT INTO federal_curriculum
            (id, title, education_level, study_week_type, version, academic_year)
        VALUES (61, 'Основной план', 'BASIC', 'FIVE_DAYS', '1', '2026/2027');
        """,
        """
        INSERT INTO federal_curriculum_requirement
            (id, federal_curriculum_id, grade, subject_id, hours, part_type)
        VALUES (71, 61, 5, 29, 3, 'MANDATORY');
        """,
        """
        INSERT INTO class_curriculum (id, schedule_id, school_class_id, subject_id, hours)
        VALUES (50, 101, 17, 29, 3);
        """,
        "INSERT INTO load_draft (schedule_id, curriculum_id) VALUES (101, 61)",
        """
        INSERT INTO public.user (id, username, password, email, corporate)
        VALUES (47, 'администратор', 'secret', 'admin@example.org', false),
            (53, 'читатель', 'secret', 'reader@example.org', false),
            (59, 'участник', 'secret', 'member@example.org', false);
        """,
        "INSERT INTO school_user (school_id, user_id) VALUES (1, 59)",
        """
        INSERT INTO user_role (user_id, role_id)
        SELECT 47, id FROM role WHERE name = 'ADMIN';
        """
    }
)
final class PsHttpTest {

    /** Isolated accounts for permission checks. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16");

    @Test
    void rendersAnAdministratorMatrixWithoutRepresentationControls(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        MatcherAssert.assertThat(
            "The matrix omitted actions or exposed representation controls",
            mvc.perform(
                MockMvcRequestBuilders.get("/permissions").accept("text/html").with(
                    SecurityMockMvcRequestPostProcessors.user(
                        new UrAcAuth(new UrAcPostgres(ctx, 47))
                    )
                )
            ).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
            Matchers.allOf(
                Matchers.containsString("Создание кабинета"),
                Matchers.containsString("Вход и регистрация"),
                Matchers.containsString("Регистрация пользователя"),
                Matchers.containsString("Пользователи школы"),
                Matchers.containsString("Добавление пользователя в школу по логину"),
                Matchers.containsString("Подписка"),
                Matchers.containsString("/schools/{school}/buildings/{building}/cabinets"),
                Matchers.containsString("Администратор"),
                Matchers.containsString("Без входа"),
                Matchers.containsString("Права доступа"),
                Matchers.not(Matchers.containsString("application/vnd.surok.permissions")),
                Matchers.not(Matchers.containsString(">JSON<")),
                Matchers.not(Matchers.containsString(">XML<"))
            )
        );
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "text/html", "application/vnd.surok.permissions.v1+json",
            "application/vnd.surok.permissions.v1+xml"
        }
    )
    void deniesTheMatrixToAnOrdinaryReader(
        final String type, @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        MatcherAssert.assertThat(
            "A non-administrator retrieved the permission matrix",
            mvc.perform(
                MockMvcRequestBuilders.get("/permissions").accept(type)
                    .header("X-Surok-Swagger", "true").with(
                        SecurityMockMvcRequestPostProcessors.user(
                            new UrAcAuth(new UrAcPostgres(ctx, 53))
                        )
                    )
            ).andReturn().getResponse().getStatus(),
            Matchers.is(HttpStatus.FORBIDDEN.value())
        );
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "text/html", "application/vnd.surok.permissions.v1+json",
            "application/vnd.surok.permissions.v1+xml"
        }
    )
    void deniesAnonymousSwaggerRequests(final String type, @Autowired final MockMvc mvc)
        throws Exception {
        MatcherAssert.assertThat(
            "Anonymous Swagger access revealed the permission matrix",
            mvc.perform(
                MockMvcRequestBuilders.get("/permissions").accept(type)
                    .header("X-Surok-Swagger", "true")
            ).andReturn().getResponse().getStatus(),
            Matchers.is(HttpStatus.UNAUTHORIZED.value())
        );
    }

    @Test
    void printsStructuredXmlForAdministrators(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        MatcherAssert.assertThat(
            "The XML matrix lost its operations or role scopes",
            mvc.perform(
                MockMvcRequestBuilders.get("/permissions")
                    .accept("application/vnd.surok.permissions.v1+xml").with(
                        SecurityMockMvcRequestPostProcessors.user(
                            new UrAcAuth(new UrAcPostgres(ctx, 47))
                        )
                    )
            ).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
            Matchers.allOf(
                Matchers.containsString("<permissions>"),
                Matchers.containsString("<operation>"),
                Matchers.containsString("<role>DIRECTOR</role>"),
                Matchers.containsString("<scope>school</scope>"),
                Matchers.containsString("<scope>denied</scope>")
            )
        );
    }

    @Test
    void documentsAllRepresentationsWithExplicitExamples(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        final JsonNode content = new ObjectMapper().readTree(
            mvc.perform(
                MockMvcRequestBuilders.get("/api-docs").with(
                    SecurityMockMvcRequestPostProcessors.user(
                        new UrAcAuth(new UrAcPostgres(ctx, 47))
                    )
                )
            ).andReturn().getResponse().getContentAsString()
        ).path("paths").path("/permissions").path("get").path("responses").path("200")
            .path("content");
        MatcherAssert.assertThat(
            "Swagger omitted a representation or its XML example",
            List.of(
                content.has("text/html"),
                content.has("application/vnd.surok.permissions.v1+json"),
                content.path("application/vnd.surok.permissions.v1+xml")
                    .path("examples").path("matrix").path("value").asText()
                    .contains("<permissions>")
            ),
            Matchers.contains(true, true, true)
        );
    }

    @Test
    void describesTheXmlRootForSwaggerExamples(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        final JsonNode document = new ObjectMapper().readTree(
            mvc.perform(
                MockMvcRequestBuilders.get("/api-docs").with(
                    SecurityMockMvcRequestPostProcessors.user(
                        new UrAcAuth(new UrAcPostgres(ctx, 47))
                    )
                )
            ).andReturn().getResponse().getContentAsString()
        );
        MatcherAssert.assertThat(
            "Swagger could not associate the XML example with its root element",
            List.of(
                document.path("paths").path("/permissions").path("get").path("responses")
                    .path("200").path("content").path("application/vnd.surok.permissions.v1+xml")
                    .path("schema").path("$ref").asText(),
                document.path("components").path("schemas").path("PermissionsXml")
                    .path("xml").path("name").asText()
            ),
            Matchers.contains("#/components/schemas/PermissionsXml", "permissions")
        );
    }

    @Test
    void hidesTheMatrixFromReaderDocumentation(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        MatcherAssert.assertThat(
            "Swagger advertised administrator permissions to an ordinary reader",
            new ObjectMapper().readTree(
                mvc.perform(
                    MockMvcRequestBuilders.get("/api-docs").with(
                        SecurityMockMvcRequestPostProcessors.user(
                            new UrAcAuth(new UrAcPostgres(ctx, 53))
                        )
                    )
                ).andReturn().getResponse().getContentAsString()
            ).path("paths").has("/permissions"),
            Matchers.is(false)
        );
    }

    @ParameterizedTest(name = "{1} {0}: {2}, memberships={7}, school={8}")
    @CsvFileSource(resources = "/permissions.csv", numLinesToSkip = 1)
    void agreesWithSwaggerAndActualHttpAuthorization(
        final ArgumentsAccessor arguments, @Autowired final MockMvc mvc,
        @Autowired final DSLContext ctx
    ) throws Exception {
        final Roles.RoleEnum role = Roles.RoleEnum.valueOf(arguments.getString(2));
        final UrAcPostgres actor = new UrAcPostgres(ctx, 53);
        if (List.of(
            Roles.RoleEnum.ADMIN, Roles.RoleEnum.BASIC_MAKER,
            Roles.RoleEnum.ADVANCED_MAKER, Roles.RoleEnum.PRO_MAKER
        ).contains(role)) {
            actor.roles().grant(role);
        }
        if (!arguments.getString(7).isEmpty()) {
            for (final String membership : arguments.getString(7).split(";", -1)) {
                final String[] fields = membership.split(":", 2);
                final UrsPostgres members = new UrsPostgres(ctx, Long.valueOf(fields[0]));
                members.add(actor);
                members.roles(actor).grant(Roles.RoleEnum.valueOf(fields[1]));
            }
        }
        final List<String> scopes = StreamSupport.stream(
            new ObjectMapper().readTree(
                mvc.perform(
                    MockMvcRequestBuilders.get("/permissions")
                        .accept("application/vnd.surok.permissions.v1+json").with(
                            SecurityMockMvcRequestPostProcessors.user(
                                new UrAcAuth(new UrAcPostgres(ctx, 47))
                            )
                        )
                ).andReturn().getResponse().getContentAsString()
            ).path("operations").spliterator(), false
        ).filter(
            operation -> arguments.getString(0).equals(operation.path("path").asText())
                && arguments.getString(1).equals(operation.path("method").asText())
        ).flatMap(
            operation -> StreamSupport.stream(operation.path("access").spliterator(), false)
        ).filter(
            grant -> arguments.getString(2).equals(grant.path("role").asText())
        ).map(grant -> grant.path("scope").asText()).toList();
        final MockHttpServletRequestBuilder description = MockMvcRequestBuilders.get("/api-docs");
        final MockHttpServletRequestBuilder request = MockMvcRequestBuilders.request(
            HttpMethod.valueOf(arguments.getString(1)),
            arguments.getString(0).replace("{school}", arguments.getString(8))
                .replace("{building}", "10").replace("{schedule}", "101")
                .replace("{load}", "50").replace("{curriculum}", "61")
                .replace("{requirement}", "71").replace("{user}", "59")
        ).accept(arguments.getString(4)).contentType(arguments.getString(4))
            .content(arguments.getString(5)).header("X-Surok-Swagger", "true")
            .with(SecurityMockMvcRequestPostProcessors.csrf());
        if (role != Roles.RoleEnum.ANONYMOUS) {
            description.with(SecurityMockMvcRequestPostProcessors.user(new UrAcAuth(actor)));
            request.with(SecurityMockMvcRequestPostProcessors.user(new UrAcAuth(actor)));
        }
        final MockHttpServletResponse swagger = mvc.perform(description).andReturn().getResponse();
        MatcherAssert.assertThat(
            "The matrix, Swagger visibility and HTTP authorization disagreed",
            List.of(
                scopes, swagger.getStatus(),
                new ObjectMapper().readTree(swagger.getContentAsString()).path("paths")
                    .path(arguments.getString(0)).has(
                        arguments.getString(1).toLowerCase(Locale.ROOT)
                    ),
                mvc.perform(request).andReturn().getResponse().getStatus()
            ),
            Matchers.contains(
                List.of(arguments.getString(3)), HttpStatus.OK.value(),
                !"denied".equals(arguments.getString(3)), arguments.getInteger(6)
            )
        );
    }

    @ParameterizedTest
    @CsvSource({"ADMIN, all", "DIRECTOR, school", "DEPUTY_DIRECTOR, school"})
    void rechecksRevokedRolesWithTheSameAuthentication(
        final ArgumentsAccessor arguments,
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        final Roles.RoleEnum role = Roles.RoleEnum.valueOf(arguments.getString(0));
        final UrAcPostgres actor = new UrAcPostgres(ctx, 53);
        final Roles roles;
        if (role == Roles.RoleEnum.ADMIN) {
            roles = actor.roles();
        } else {
            final UrsPostgres members = new UrsPostgres(ctx, 1L);
            members.add(actor);
            roles = members.roles(actor);
        }
        final long identity = roles.grant(role).uid();
        final UrAcAuth principal = new UrAcAuth(actor);
        final UsernamePasswordAuthenticationToken token =
            UsernamePasswordAuthenticationToken.authenticated(
                principal, "", principal.getAuthorities()
            );
        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            new SecurityContextImpl(token)
        );
        final List<List<?>> results = new ArrayList<>(2);
        for (final boolean allowed : List.of(true, false)) {
            if (!allowed) {
                roles.revoke(identity);
            }
            final List<String> scopes = StreamSupport.stream(
                new ObjectMapper().readTree(
                    mvc.perform(
                        MockMvcRequestBuilders.get("/permissions")
                            .accept("application/vnd.surok.permissions.v1+json").with(
                                SecurityMockMvcRequestPostProcessors.user(
                                    new UrAcAuth(new UrAcPostgres(ctx, 47))
                                )
                            )
                    ).andReturn().getResponse().getContentAsString()
                ).path("operations").spliterator(), false
            ).filter(
                operation -> "/schools/{school}/buildings/{building}".equals(
                    operation.path("path").asText()
                ) && "PUT".equals(operation.path("method").asText())
            ).flatMap(
                operation -> StreamSupport.stream(operation.path("access").spliterator(), false)
            ).filter(
                grant -> role.name().equals(grant.path("role").asText())
            ).map(grant -> grant.path("scope").asText()).toList();
            final MockHttpServletResponse swagger = mvc.perform(
                MockMvcRequestBuilders.get("/api-docs").with(
                    SecurityMockMvcRequestPostProcessors.authentication(token)
                )
            ).andReturn().getResponse();
            results.add(
                List.of(
                    scopes, swagger.getStatus(),
                    new ObjectMapper().readTree(swagger.getContentAsString()).path("paths")
                        .path("/schools/{school}/buildings/{building}").has("put"),
                    mvc.perform(
                        MockMvcRequestBuilders.put("/schools/1/buildings/10").session(session)
                            .accept("application/vnd.surok.building-v1+json")
                            .contentType("application/vnd.surok.building-v1+json")
                            .content("{\"name\":\"Корпус лицея\"}")
                            .with(SecurityMockMvcRequestPostProcessors.csrf())
                    ).andReturn().getResponse().getStatus(),
                    mvc.perform(
                        MockMvcRequestBuilders.put("/schools/1/buildings/10")
                            .header("X-Surok-Swagger", "true")
                            .accept("application/vnd.surok.building-v1+json")
                            .contentType("application/vnd.surok.building-v1+json")
                            .content("{\"name\":\"Корпус гимназистов\"}")
                            .with(SecurityMockMvcRequestPostProcessors.csrf()).with(
                                SecurityMockMvcRequestPostProcessors.authentication(token)
                            )
                    ).andReturn().getResponse().getStatus()
                )
            );
        }
        MatcherAssert.assertThat(
            "Revoked rights remained in Swagger or an existing authentication session",
            results,
            Matchers.contains(
                List.of(
                    List.of(arguments.getString(1)), HttpStatus.OK.value(), true,
                    HttpStatus.OK.value(), HttpStatus.OK.value()
                ),
                List.of(
                    List.of(arguments.getString(1)), HttpStatus.OK.value(), false,
                    HttpStatus.FORBIDDEN.value(), HttpStatus.FORBIDDEN.value()
                )
            )
        );
    }

    @Test
    void showsTheNavigationLinkOnlyToAdministrators(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        MatcherAssert.assertThat(
            "Permission navigation leaked to a reader or disappeared for an administrator",
            List.of(
                mvc.perform(
                    MockMvcRequestBuilders.get("/users/profile").with(
                        SecurityMockMvcRequestPostProcessors.user(
                            new UrAcAuth(new UrAcPostgres(ctx, 47))
                        )
                    )
                ).andReturn().getResponse().getContentAsString().contains("href=\"/permissions\""),
                mvc.perform(
                    MockMvcRequestBuilders.get("/users/profile").with(
                        SecurityMockMvcRequestPostProcessors.user(
                            new UrAcAuth(new UrAcPostgres(ctx, 53))
                        )
                    )
                ).andReturn().getResponse().getContentAsString().contains("href=\"/permissions\"")
            ),
            Matchers.contains(true, false)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"BASIC_MAKER", "ADVANCED_MAKER", "PRO_MAKER"})
    void deniesEverySubscriptionRole(
        final Roles.RoleEnum role, @Autowired final MockMvc mvc,
        @Autowired final DSLContext ctx
    ) throws Exception {
        final UrAcPostgres actor = new UrAcPostgres(ctx, 53);
        actor.roles().grant(role);
        MatcherAssert.assertThat(
            "A subscription role opened the permission page",
            mvc.perform(
                MockMvcRequestBuilders.get("/permissions").accept("text/html").with(
                    SecurityMockMvcRequestPostProcessors.user(new UrAcAuth(actor))
                )
            ).andReturn().getResponse().getStatus(),
            Matchers.is(HttpStatus.FORBIDDEN.value())
        );
    }

    @Test
    void rejectsUnversionedJsonForTheMatrix(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        MatcherAssert.assertThat(
            "The permission resource accepted an unsupported representation",
            mvc.perform(
                MockMvcRequestBuilders.get("/permissions").accept("application/json").with(
                    SecurityMockMvcRequestPostProcessors.user(
                        new UrAcAuth(new UrAcPostgres(ctx, 47))
                    )
                )
            ).andReturn().getResponse().getStatus(),
            Matchers.is(HttpStatus.NOT_ACCEPTABLE.value())
        );
    }

    /**
     * Apply the real schema to the isolated database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PsHttpTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", PsHttpTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", PsHttpTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
