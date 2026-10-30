/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school;

import com.eshabakhov.surok.User;
import com.eshabakhov.surok.user.AuthUser;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrPostgres;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * School HTTP negotiation, authorization and mutation behavior.
 *
 * @since 0.0.1
 * @checkstyle NoJavadocForPrivateMethodsCheck (1000 lines)
 * @checkstyle ProhibitFieldsInTestClassesCheck (1000 lines)
 */
@Testcontainers
@DirtiesContext
@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings({
    "PMD.UnusedPrivateMethod", "UnusedMethod",
    "PMD.UnitTestShouldIncludeAssert"
})
final class SlsHttpTest {

    /** Postgres container. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("surok")
        .withUsername("test")
        .withPassword("test");

    /** Database context. */
    private final DSLContext ctx;

    /** HTTP client with security filters. */
    private final MockMvc mvc;

    @Autowired
    SlsHttpTest(final DSLContext ctx, final MockMvc mvc) {
        this.ctx = ctx;
        this.mvc = mvc;
    }

    /**
     * Verifies school formats share a single resource.
     *
     * @param path Request suffix
     * @param resource Representation resource
     * @param format Serialization format
     */
    @ParameterizedTest
    @CsvSource({
        "'', schools.v1, json", "/fragment, schools.v1, xml", "/3, school-v1, json",
        "/3/edit, school-v1, xml"
    })
    void negotiatesVersionedSchoolReads(
        final String path, final String resource,
        final String format
    ) throws Exception {
        this.prepare();
        final String type = String.format("application/vnd.surok.%s+%s", resource, format);
        this.mvc.perform(
            MockMvcRequestBuilders.get(String.format("/schools%s", path)).with(
                SecurityMockMvcRequestPostProcessors.user(this.principal(Roles.RoleEnum.ADMIN))
            ).accept(type)
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(type));
    }

    /**
     * Verifies removed school API routes cannot handle authenticated requests.
     *
     * @param path Legacy resource suffix
     * @throws Exception If the HTTP request fails
     */
    @ParameterizedTest
    @ValueSource(strings = {"", "/3"})
    void removesLegacySchoolRoutes(final String path) throws Exception {
        this.prepare();
        this.mvc.perform(
            MockMvcRequestBuilders.get(String.format("/api/schools%s", path)).with(
                SecurityMockMvcRequestPostProcessors.user(this.principal(Roles.RoleEnum.ADMIN))
            )
        ).andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    /**
     * Verifies shared navigation keeps the schools link.
     *
     * @param path Page suffix
     */
    @ParameterizedTest
    @ValueSource(strings = {"", "/create", "/3", "/3/edit"})
    void rendersSchoolPagesWithNavigation(final String path) throws Exception {
        this.prepare();
        this.mvc.perform(
            MockMvcRequestBuilders.get(String.format("/schools%s", path)).with(
                SecurityMockMvcRequestPostProcessors.user(this.principal(Roles.RoleEnum.ADMIN))
            )
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content()
            .string(Matchers.containsString("href=\"/schools\""))
        );
    }

    /** Verifies unsupported representations are rejected before database writes. */
    @Test
    void rejectsUnsupportedAcceptBeforeCreatingSchools() throws Exception {
        this.prepare();
        this.mvc.perform(
            MockMvcRequestBuilders.post("/schools/create")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("name", "Лишняя").accept("application/pdf")
            .with(SecurityMockMvcRequestPostProcessors.csrf()).with(
                SecurityMockMvcRequestPostProcessors.user(this.principal(Roles.RoleEnum.ADMIN))
            )
        ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
        MatcherAssert.assertThat(
            "Rejected school request changed the database",
            this.ctx.fetchCount(org.jooq.impl.DSL.table("school")), Matchers.equalTo(2)
        );
    }

    /**
     * Verifies an unknown identifier never creates a school through PUT.
     *
     * @param id Missing school identifier
     */
    @ParameterizedTest
    @ValueSource(longs = {0, 99_999})
    void rejectsUnknownSchoolUpdatesWithoutCreatingRows(final long id) throws Exception {
        this.prepare();
        this.mvc.perform(
            MockMvcRequestBuilders.put(String.format("/schools/%d", id))
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Лишняя\"}")
            .accept("application/vnd.surok.school-v1+json")
            .with(SecurityMockMvcRequestPostProcessors.csrf()).with(
                SecurityMockMvcRequestPostProcessors.user(this.principal(Roles.RoleEnum.ADMIN))
            )
        ).andExpect(MockMvcResultMatchers.status().isNotFound());
        MatcherAssert.assertThat(
            "Unknown school update created a row",
            this.ctx.fetchCount(org.jooq.impl.DSL.table("school")), Matchers.equalTo(2)
        );
    }

    /** Verifies JSON creation uses validated input and a canonical Location. */
    @Test
    void createsVersionedSchoolsFromJson() throws Exception {
        this.prepare();
        this.mvc.perform(
            MockMvcRequestBuilders.post("/schools")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"  Лицей №42  \"}")
            .accept("application/vnd.surok.school-v1+json")
            .with(SecurityMockMvcRequestPostProcessors.csrf()).with(
                SecurityMockMvcRequestPostProcessors.user(this.principal(Roles.RoleEnum.ADMIN))
            )
        ).andExpect(MockMvcResultMatchers.status().isCreated()).andExpect(
            MockMvcResultMatchers.header().string(
                "Location",
                Matchers.matchesPattern("/schools/[0-9]+")
            )
        ).andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Лицей №42"));
    }

    /** Verifies the browser navigates to the saved school. */
    @Test
    void redirectsAfterCreatingSchoolsFromForms() throws Exception {
        this.prepare();
        this.mvc.perform(
            MockMvcRequestBuilders.post("/schools/create")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("name", "Лицей").accept(MediaType.TEXT_HTML)
            .with(SecurityMockMvcRequestPostProcessors.csrf()).with(
                SecurityMockMvcRequestPostProcessors.user(this.principal(Roles.RoleEnum.ADMIN))
            )
        ).andExpect(MockMvcResultMatchers.status().isFound()).andExpect(
            MockMvcResultMatchers.header().string(
                "Location",
                Matchers.matchesPattern("/schools/[0-9]+")
            )
        );
    }

    /** Verifies staff cannot access another school. */
    @Test
    void deniesReadingAnotherSchool() throws Exception {
        this.prepare();
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/4").with(
                SecurityMockMvcRequestPostProcessors.user(this.principal(Roles.RoleEnum.DIRECTOR))
            )
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    /** Verifies staff cannot list all schools. */
    @Test
    void restrictsSchoolCollectionsToAdministrators() throws Exception {
        this.prepare();
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools").with(
                SecurityMockMvcRequestPostProcessors.user(this.principal(Roles.RoleEnum.DIRECTOR))
            )
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    /** Verifies staff can see their school and nested navigation. */
    @Test
    void allowsReadingOwnSchool() throws Exception {
        this.prepare();
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/3").with(
                SecurityMockMvcRequestPostProcessors.user(this.principal(Roles.RoleEnum.DIRECTOR))
            )
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content()
            .string(Matchers.containsString("href=\"/schools/3/schedules\""))
        );
    }

    /** Verifies mutations require CSRF protection. */
    @Test
    void rejectsSchoolRemovalWithoutCsrf() throws Exception {
        this.prepare();
        this.mvc.perform(
            MockMvcRequestBuilders.delete("/schools/3").with(
                SecurityMockMvcRequestPostProcessors.user(this.principal(Roles.RoleEnum.ADMIN))
            )
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    /**
     * Verifies nested XSLT pages accept the printed school model.
     *
     * @param path Page suffix
     */
    @ParameterizedTest
    @ValueSource(strings = {"/teachers", "/subjects", "/classes", "/schedules"})
    void preservesNestedSchoolPages(final String path) throws Exception {
        this.prepare();
        this.mvc.perform(
            MockMvcRequestBuilders.get(String.format("/schools/3%s", path))
                .queryParam("unused", "ignored").with(
                    SecurityMockMvcRequestPostProcessors.user(
                        this.principal(Roles.RoleEnum.DIRECTOR)
                    )
                )
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().string(
                Matchers.containsString(
                    String.format("rel=\"canonical\" href=\"https://surok.com/schools/3%s\"", path)
                )
            )
        );
    }

    /** Prepares independent school fixtures before a scenario. */
    private void prepare() {
        this.ctx.execute("TRUNCATE TABLE public.school, subject RESTART IDENTITY CASCADE");
        this.ctx.execute(
            """
            INSERT INTO public.school (id, name, is_deleted)
            VALUES (3, 'Школа №3', false), (4, 'Школа №4', false)
            """
        );
    }

    /**
     * Creates an authenticated wrapper around a persisted school user.
     *
     * @param role User role
     * @return School principal
     * @throws Exception If role creation fails
     */
    private AuthUser principal(final Roles.RoleEnum role) throws Exception {
        final User user = new UrPostgres(
            this.ctx,
            this.ctx.resultQuery(
                """
                INSERT INTO public."user" (username, password, email, corporate, school_id)
                VALUES (?, 'secret', ?, true, ?) RETURNING id
                """,
                UUID.randomUUID().toString(),
                String.format("%s@example.org", UUID.randomUUID()),
                3L
            ).fetchSingle("id", Long.class)
        );
        user.roles().grant(role);
        return new AuthUser(user);
    }

    /**
     * Configures the disposable database.
     *
     * @param registry Test properties
     */
    @DynamicPropertySource
    private static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.liquibase.enabled", () -> true);
        registry.add("spring.datasource.url", SlsHttpTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", SlsHttpTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", SlsHttpTest.POSTGRES::getPassword);
    }
}
