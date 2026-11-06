/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement.http;

import com.eshabakhov.surok.User;
import com.eshabakhov.surok.federal.curriculum.requirement.Requirement;
import com.eshabakhov.surok.federal.curriculum.requirement.RqsPostgres;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInputHttp;
import com.eshabakhov.surok.user.AuthUser;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrPostgres;
import java.util.List;
import java.util.Map;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Both requirement kinds share resources and restrict changes to administrators.
 *
 * @since 0.0.1
 * @checkstyle ProhibitFieldsInTestClassesCheck (1000 lines)
 */
@Testcontainers
@DirtiesContext
@SpringBootTest
@AutoConfigureMockMvc
@Sql(
    statements = {
        "TRUNCATE TABLE federal_curriculum RESTART IDENTITY CASCADE",
        """
        INSERT INTO federal_curriculum
            (id, title, education_level, study_week_type, version, academic_year, is_deleted)
        VALUES (17, 'Основной план', 'BASIC', 'FIVE_DAYS', '1', '2026/2027', false);
        """
    }
)
@SuppressWarnings({"PMD.UnusedPrivateMethod", "UnusedMethod"})
final class RqsHttpTest {

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
    RqsHttpTest(final DSLContext ctx, final MockMvc mvc) {
        this.ctx = ctx;
        this.mvc = mvc;
    }

    @Test
    void createsMandatoryHoursFromTheSubjectSelection() throws Exception {
        this.ctx.execute(
            """
            INSERT INTO subject (id, name, origin) VALUES (517, 'Алгебра №517', 'FEDERAL')
            ON CONFLICT (id) DO NOTHING
            """
        );
        MatcherAssert.assertThat(
            "Selecting a federal subject failed to save mandatory hours through the common form",
            List.of(
                this.mvc.perform(
                    this.authorized(
                        MockMvcRequestBuilders.post("/federal/curriculums/17/requirements"),
                        Roles.RoleEnum.ADMIN
                    ).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grade", "5").param("selection", "517").param("hours", "7")
                        .accept(MediaType.TEXT_HTML)
                ).andReturn().getResponse().getStatus(),
                this.ctx.fetch(
                    "SELECT grade, subject_id, hours, part_type FROM federal_curriculum_requirement"
                ).intoMaps()
            ),
            Matchers.contains(
                302,
                List.of(
                    Map.of("grade", 5, "subject_id", 517L, "hours", 7, "part_type", "MANDATORY")
                )
            )
        );
    }

    @Test
    void savesAndRestoresTheExplicitMaximum() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/federal/curriculums/17/requirements"),
                Roles.RoleEnum.ADMIN
            ).contentType("application/vnd.surok.federal-curriculum-requirement-v1+json")
                .accept("application/vnd.surok.federal-curriculum-requirement-v1+json")
                .content("{\"grade\":5,\"partType\":\"OPTIONAL\",\"hours\":3}")
        );
        MatcherAssert.assertThat(
            "The requirement did not preserve the explicit maximum",
            this.ctx.fetchValue(
                "SELECT hours - 1 FROM federal_curriculum_requirement"
            ),
            Matchers.is(2)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"json", "xml"})
    void letsReadersRetrieveAnOptionalRequirement(final String format) throws Exception {
        MatcherAssert.assertThat(
            "A reader could not retrieve a variative requirement",
            this.mvc.perform(
                MockMvcRequestBuilders.get(
                    "/federal/curriculums/17/requirements/{id}", this.optional(5, 0).uid()
                ).session(this.session(Roles.RoleEnum.VIEWER)).accept(
                    String.format(
                        "application/vnd.surok.federal-curriculum-requirement-v1+%s", format
                    )
                )
            ).andReturn().getResponse().getStatus(),
            Matchers.is(200)
        );
    }

    @ParameterizedTest
    @CsvSource({"5,0,302", "5,4,302", "5,-1,400", "12,3,400", "5,1.5,400"})
    void validatesTheOptionalRequirementForm(
        final String grade, final String hours, final int status
    ) throws Exception {
        MatcherAssert.assertThat(
            "The requirement form accepted an invalid maximum or rejected valid hours",
            this.mvc.perform(
                this.authorized(
                    MockMvcRequestBuilders.post("/federal/curriculums/17/requirements"),
                    Roles.RoleEnum.ADMIN
                ).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("grade", grade).param("selection", "OPTIONAL")
                    .param("hours", hours).accept(MediaType.TEXT_HTML)
            ).andReturn().getResponse().getStatus(),
            Matchers.is(status)
        );
    }

    @Test
    void rejectsReaderChangesWithValidCsrf() throws Exception {
        MatcherAssert.assertThat(
            "A reader created a federal requirement with a valid CSRF token",
            this.mvc.perform(
                this.authorized(
                    MockMvcRequestBuilders.post("/federal/curriculums/17/requirements"),
                    Roles.RoleEnum.VIEWER
                ).contentType("application/vnd.surok.federal-curriculum-requirement-v1+json")
                    .accept("application/vnd.surok.federal-curriculum-requirement-v1+json").content(
                        "{\"grade\":5,\"partType\":\"OPTIONAL\",\"hours\":7}"
                    )
            ).andReturn().getResponse().getStatus(),
            Matchers.is(403)
        );
    }

    @Test
    void negotiatesBeforeChangingTheLimit() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.put(
                    "/federal/curriculums/17/requirements/{id}", this.optional(5, 3).uid()
                ), Roles.RoleEnum.ADMIN
            ).contentType("application/vnd.surok.federal-curriculum-requirement-v1+json")
                .accept(MediaType.APPLICATION_JSON)
                .content("{\"grade\":5,\"partType\":\"OPTIONAL\",\"hours\":7}")
        );
        MatcherAssert.assertThat(
            "An unsupported Accept header allowed changing the maximum",
            this.ctx.fetchValue(
                "SELECT hours - 0 FROM federal_curriculum_requirement"
            ),
            Matchers.is(3)
        );
    }

    @Test
    void revisesAnOptionalRequirementThroughTheCommonResource() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.put(
                    "/federal/curriculums/17/requirements/{id}", this.optional(5, 3).uid()
                ), Roles.RoleEnum.ADMIN
            ).contentType("application/vnd.surok.federal-curriculum-requirement-v1+json")
                .accept("application/vnd.surok.federal-curriculum-requirement-v1+json")
                .content("{\"grade\":5,\"partType\":\"OPTIONAL\",\"hours\":0}")
        );
        MatcherAssert.assertThat(
            "Updating the requirement failed to store an explicit zero maximum",
            this.ctx.fetchValue(
                "SELECT hours - 0 FROM federal_curriculum_requirement"
            ),
            Matchers.is(0)
        );
    }

    @ParameterizedTest
    @CsvSource({"PRIMARY, 5", "BASIC, 4", "BASIC, 10", "SECONDARY, 9"})
    void rejectsApiWritesOutsideTheEducationLevel(final String level, final int grade)
        throws Exception {
        this.ctx.execute(
            "UPDATE federal_curriculum SET education_level = ?::education_level_type WHERE id = 17",
            level
        );
        MatcherAssert.assertThat(
            "The API accepted a requirement outside its education level",
            this.mvc.perform(
                this.authorized(
                    MockMvcRequestBuilders.post("/federal/curriculums/17/requirements"),
                    Roles.RoleEnum.ADMIN
                ).contentType("application/vnd.surok.federal-curriculum-requirement-v1+json")
                    .accept("application/vnd.surok.federal-curriculum-requirement-v1+json").content(
                        String.format(
                            "{\"grade\":%d,\"partType\":\"OPTIONAL\",\"hours\":3}", grade
                        )
                    )
            ).andReturn().getResponse().getStatus(),
            Matchers.is(400)
        );
    }

    @Test
    void rejectsConflictingGradeChanges() throws Exception {
        final Requirement selected = this.optional(5, 3);
        this.optional(6, 2);
        MatcherAssert.assertThat(
            "Changing a grade overwrote another optional requirement or failed as a server error",
            this.mvc.perform(
                this.authorized(
                    MockMvcRequestBuilders.put(
                        "/federal/curriculums/17/requirements/{id}", selected.uid()
                    ), Roles.RoleEnum.ADMIN
                ).contentType("application/vnd.surok.federal-curriculum-requirement-v1+json")
                    .accept("application/vnd.surok.federal-curriculum-requirement-v1+json").content(
                        "{\"grade\":6,\"partType\":\"OPTIONAL\",\"hours\":7}"
                    )
            ).andReturn().getResponse().getStatus(),
            Matchers.is(409)
        );
    }

    @Test
    void deletesAnOptionalRequirementThroughTheCommonResource() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.delete(
                    "/federal/curriculums/17/requirements/{id}", this.optional(5, 3).uid()
                ), Roles.RoleEnum.ADMIN
            ).accept("application/vnd.surok.federal-curriculum-requirement-v1+json")
        );
        MatcherAssert.assertThat(
            "Deleting the optional requirement left an active grade maximum",
            new RqsPostgres(this.ctx, 17L).optional(5),
            Matchers.emptyIterable()
        );
    }

    private Requirement optional(final int grade, final int hours) throws Exception {
        return new RqsPostgres(this.ctx, 17L).create(
            new RqInputHttp(
                Map.of(
                    "partType", "OPTIONAL", "grade", Integer.toString(grade),
                    "hours", Integer.toString(hours)
                )
            )
        );
    }

    private MockHttpServletRequestBuilder authorized(
        final MockHttpServletRequestBuilder request, final Roles.RoleEnum role
    ) throws Exception {
        final MockHttpSession session = this.session(role);
        final MvcResult page = this.mvc.perform(
            MockMvcRequestBuilders.get("/federal/subjects").session(session)
        ).andReturn();
        final CsrfToken token = (CsrfToken) page.getRequest().getAttribute(
            CsrfToken.class.getName()
        );
        return request.session(session).cookie(page.getResponse().getCookies())
            .header(token.getHeaderName(), token.getToken());
    }

    private MockHttpSession session(final Roles.RoleEnum role) throws Exception {
        final User user = new UrPostgres(
            this.ctx,
            this.ctx.resultQuery(
                """
                INSERT INTO public."user" (username, password, email, corporate)
                VALUES (?, 'secret', ?, true) RETURNING id
                """,
                UUID.randomUUID().toString(),
                String.format("%s@example.org", UUID.randomUUID())
            ).fetchSingle("id", Long.class)
        );
        if (role != Roles.RoleEnum.VIEWER) {
            user.roles().grant(role);
        }
        final AuthUser principal = new AuthUser(user);
        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            new SecurityContextImpl(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
            )
        );
        return session;
    }

    @DynamicPropertySource
    private static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.liquibase.enabled", () -> true);
        registry.add("spring.datasource.url", RqsHttpTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", RqsHttpTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", RqsHttpTest.POSTGRES::getPassword);
    }
}
