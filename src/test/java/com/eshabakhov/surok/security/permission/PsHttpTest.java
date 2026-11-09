/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.User;
import com.eshabakhov.surok.user.AuthUser;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrPostgres;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Administrator access to the permission matrix and its negotiated representations.
 * For example, a viewer cannot retrieve the XML matrix through Swagger.
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
        """
        INSERT INTO public.user (id, username, password, email, corporate)
        VALUES (47, 'администратор', 'secret', 'admin@example.org', false),
            (53, 'читатель', 'secret', 'reader@example.org', false);
        """,
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
                MockMvcRequestBuilders.get("/permissions").accept(MediaType.TEXT_HTML).with(
                    SecurityMockMvcRequestPostProcessors.user(new AuthUser(new UrPostgres(ctx, 47)))
                )
            ).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
            Matchers.allOf(
                Matchers.containsString("Создание кабинета"),
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
                            new AuthUser(new UrPostgres(ctx, 53))
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
                            new AuthUser(new UrPostgres(ctx, 47))
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
                    SecurityMockMvcRequestPostProcessors.user(new AuthUser(new UrPostgres(ctx, 47)))
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
                    SecurityMockMvcRequestPostProcessors.user(new AuthUser(new UrPostgres(ctx, 47)))
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
                            new AuthUser(new UrPostgres(ctx, 53))
                        )
                    )
                ).andReturn().getResponse().getContentAsString()
            ).path("paths").has("/permissions"),
            Matchers.is(false)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "/schools/{school}, PUT, DIRECTOR, school",
        "/schools/{school}, PUT, BASIC_MAKER, denied",
        "/schools/{school}, DELETE, ADMIN, all",
        "/schools/{school}/users, GET, VIEWER, school",
        "/schools/{school}/users, POST, DIRECTOR, school",
        "/schools/{school}/users, POST, VIEWER, denied",
        "/schools/{school}/users/{user}/roles, POST, DIRECTOR, school",
        "/schools/{school}/users/{user}/roles, POST, BASIC_MAKER, denied",
        "/schools/{school}/buildings, GET, VIEWER, school",
        "/schools/{school}/buildings, POST, DEPUTY_DIRECTOR, school",
        "/schools/{school}/buildings, POST, BASIC_MAKER, denied",
        "/schools/{school}/buildings/{building}/cabinets, POST, DIRECTOR, school",
        "/schools/{school}/buildings/{building}/cabinets, POST, VIEWER, denied",
        "/schools/{school}/buildings/{building}/cabinets, POST, BASIC_MAKER, denied",
        "/schools/{school}/buildings/{building}/cabinets, POST, ADMIN, all",
        "/schools/{school}/buildings/{building}/cabinets, GET, VIEWER, school",
        "/schools/{school}/classes, GET, VIEWER, school",
        "/schools/{school}/classes, POST, DEPUTY_DIRECTOR, school",
        "/schools/{school}/classes, POST, BASIC_MAKER, denied",
        "/schools/{school}/teachers, GET, VIEWER, school",
        "/schools/{school}/teachers, POST, DIRECTOR, school",
        "/schools/{school}/teachers, POST, BASIC_MAKER, denied",
        "/schools/{school}/schedules, GET, VIEWER, school",
        "/schools/{school}/schedules, POST, DEPUTY_DIRECTOR, school",
        "/schools/{school}/schedules, POST, BASIC_MAKER, denied",
        "/schools/{school}/schedules/{schedule}/loads, GET, VIEWER, school",
        "/schools/{school}/schedules/{schedule}/loads/{load:[0-9]+}, GET, ADMIN, all",
        "/schools/{school}/schedules/{schedule}/loads/draft, GET, VIEWER, school",
        "/schools/{school}/schedules/{schedule}/loads/draft, PUT, DIRECTOR, school",
        "/schools/{school}/schedules/{schedule}/loads/draft, PUT, BASIC_MAKER, denied",
        "/schools/{school}/schedules/{schedule}/loads/draft/application, POST, VIEWER, denied",
        "/federal/subjects, POST, DIRECTOR, denied",
        "/federal/subjects, POST, ADMIN, all",
        "/schools/{school}/subjects, POST, DIRECTOR, school",
        "/schools/{school}/subjects, POST, BASIC_MAKER, denied",
        "/schools/{school}/subjects, GET, VIEWER, school",
        "/federal/curriculums, GET, VIEWER, all",
        "/federal/curriculums, POST, VIEWER, denied",
        "/federal/curriculums/{curriculum}, GET, VIEWER, all",
        "/federal/curriculums/{curriculum}, DELETE, VIEWER, denied",
        "/federal/curriculums/{curriculum}/requirements, GET, VIEWER, all",
        "/federal/curriculums/{curriculum}/requirements, POST, VIEWER, denied",
        "/federal/curriculums/{curriculum}/requirements, POST, ADMIN, all",
        "/federal/curriculums/{curriculum}/requirements/{requirement}, PUT, VIEWER, denied",
        "/api/users/{user}/roles, GET, ADMIN, all",
        "/api/users/{user}/roles, GET, VIEWER, denied",
        "/api/users/{user}/roles, POST, VIEWER, denied",
        "/permissions, GET, ADMIN, all",
        "/permissions, GET, VIEWER, denied",
        "/users/login, GET, ANONYMOUS, all"
    })
    void distinguishesRoleAndSchoolRestrictions(
        final ArgumentsAccessor arguments, @Autowired final MockMvc mvc,
        @Autowired final DSLContext ctx
    ) throws Exception {
        final JsonNode document = new ObjectMapper().readTree(
            mvc.perform(
                MockMvcRequestBuilders.get("/permissions")
                    .accept("application/vnd.surok.permissions.v1+json").with(
                        SecurityMockMvcRequestPostProcessors.user(
                            new AuthUser(new UrPostgres(ctx, 47))
                        )
                    )
            ).andReturn().getResponse().getContentAsString()
        );
        final Collection<String> scopes = new ArrayList<>(1);
        for (final JsonNode operation : document.path("operations")) {
            if (arguments.getString(0).equals(operation.path("path").asText())
                && arguments.getString(1).equals(operation.path("method").asText())) {
                for (final JsonNode grant : operation.path("access")) {
                    if (arguments.getString(2).equals(grant.path("role").asText())) {
                        scopes.add(grant.path("scope").asText());
                    }
                }
            }
        }
        MatcherAssert.assertThat(
            "The permission matrix diverged from controller authorization",
            scopes.stream().distinct().toList(),
            Matchers.contains(arguments.getString(3))
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
                            new AuthUser(new UrPostgres(ctx, 47))
                        )
                    )
                ).andReturn().getResponse().getContentAsString().contains("href=\"/permissions\""),
                mvc.perform(
                    MockMvcRequestBuilders.get("/users/profile").with(
                        SecurityMockMvcRequestPostProcessors.user(
                            new AuthUser(new UrPostgres(ctx, 53))
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
        final User actor = new UrPostgres(ctx, 53);
        actor.roles().grant(role);
        MatcherAssert.assertThat(
            "A subscription role opened the permission page",
            mvc.perform(
                MockMvcRequestBuilders.get("/permissions").accept(MediaType.TEXT_HTML).with(
                    SecurityMockMvcRequestPostProcessors.user(new AuthUser(actor))
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
                MockMvcRequestBuilders.get("/permissions").accept(MediaType.APPLICATION_JSON).with(
                    SecurityMockMvcRequestPostProcessors.user(new AuthUser(new UrPostgres(ctx, 47)))
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
