/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum;

import com.eshabakhov.surok.User;
import com.eshabakhov.surok.user.AuthUser;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrPostgres;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Shared Accept negotiation in federal curriculum and requirement HTTP resources.
 *
 * @since 0.0.1
 * @checkstyle ProhibitFieldsInTestClassesCheck (1000 lines)
 */
@Testcontainers
@DirtiesContext
@SpringBootTest
@Sql(
    statements = """
        INSERT INTO subject (id, origin, name)
        VALUES (516, 'FEDERAL', 'After'),
        (504, 'FEDERAL', 'Mathematics')
        ON CONFLICT DO NOTHING;
        """
)
@AutoConfigureMockMvc
@SuppressWarnings({
    "PMD.UnusedPrivateMethod", "UnusedMethod",
    "PMD.UnitTestShouldIncludeAssert"
})
final class CrsHttpTest {

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
    CrsHttpTest(final DSLContext ctx, final MockMvc mvc) {
        this.ctx = ctx;
        this.mvc = mvc;
    }

    @BeforeEach
    void insertCurriculumAndRequirement() {
        this.ctx.resultQuery(
            """
                INSERT INTO public.federal_curriculum (id, title, education_level, study_week_type,
                version, academic_year, description, is_deleted)
                VALUES (DEFAULT, 'Basic plan', 'BASIC', 'FIVE_DAYS', '1', '2026/2027',
                    'Description', false)
                """
        ).execute();
        this.ctx.resultQuery(
            """
                INSERT INTO public.federal_curriculum_requirement (id, federal_curriculum_id, grade,
                subject_id, hours, part_type, is_deleted)
                VALUES (7, 1, 5, (SELECT id FROM subject WHERE name = 'Mathematics'
                AND NOT is_deleted), 5, 'MANDATORY', false)
                """
        ).execute();
    }

    @AfterEach
    void truncateCurriculums() {
        this.ctx.resultQuery(
            """
                TRUNCATE TABLE public.federal_curriculum RESTART IDENTITY CASCADE
                """
        ).execute();
    }

    /**
     * Verifies translated levels reach page lists, details and forms.
     *
     * @param path Curriculum route
     * @param type Accepted representation
     * @param expected Visible title or serialized field
     * @throws Exception If the request fails
     */
    @ParameterizedTest
    @CsvSource({
        "'', text/html, Основное общее образование",
        "/fragment, text/html, Основное общее образование",
        "/1, text/html, Основное общее образование",
        "/create, text/html, Начальное общее образование",
        "/1/edit, text/html, Среднее общее образование"
    })
    void displaysRussianEducationLevels(
        final String path, final String type, final String expected
    ) throws Exception {
        MatcherAssert.assertThat(
            "The curriculum representation omitted the Russian education level",
            this.mvc.perform(
                MockMvcRequestBuilders.get(String.format("/federal/curriculums%s", path))
                    .session(this.session()).header("Accept", type)
            ).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
            Matchers.containsString(expected)
        );
    }

    /**
     * Verifies translated weeks reach page lists, details and forms.
     *
     * @param path Curriculum route
     * @param type Accepted representation
     * @param expected Visible title or serialized field
     * @throws Exception If the request fails
     */
    @ParameterizedTest
    @CsvSource({
        "'', text/html, Пятидневная",
        "/fragment, text/html, Пятидневная",
        "/1, text/html, Пятидневная",
        "/create, text/html, Шестидневная",
        "/1/edit, text/html, Шестидневная"
    })
    void displaysRussianStudyWeeks(
        final String path, final String type, final String expected
    ) throws Exception {
        MatcherAssert.assertThat(
            "The curriculum representation omitted the Russian study week",
            this.mvc.perform(
                MockMvcRequestBuilders.get(String.format("/federal/curriculums%s", path))
                    .session(this.session()).header("Accept", type)
            ).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
            Matchers.containsString(expected)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "/1, federal-curriculum-v1, json",
        "/1, federal-curriculum-v2, json",
        "/1/edit, federal-curriculum-v1, xml",
        "/1/edit, federal-curriculum-v2, xml",
        "'', federal-curriculums.v1, json",
        "/fragment, federal-curriculums.v1, xml",
        "/1/requirements, federal-curriculum-requirements.v1, json",
        "/1/requirements/fragment, federal-curriculum-requirements.v1, xml",
        "/1/requirements/7, federal-curriculum-requirement-v1, json",
        "/1/requirements/7, federal-curriculum-requirement-v1, xml"
    })
    void negotiatesVersionedRead(
        final String path, final String resource, final String format
    ) throws Exception {
        final String expected = String.format("application/vnd.surok.%s+%s", resource, format);
        this.mvc.perform(
            MockMvcRequestBuilders.get(String.format("/federal/curriculums%s", path))
                .session(this.session())
                .header("Accept", String.format("text/html;q=0.1,%s;q=0.9", expected))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().contentTypeCompatibleWith(expected)
        ).andExpect(MockMvcResultMatchers.content().string(Matchers.not(Matchers.emptyString())));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"*/*", "text/html;q=0.2,application/json;q=0.9"})
    void defaultsHtmlPagesToHtml(final String header) throws Exception {
        for (final String path : List.of(
            "", "/create", "/fragment", "/1", "/1/edit",
            "/1/requirements", "/1/requirements/fragment"
        )) {
            final MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(
                String.format("/federal/curriculums%s", path)
            ).session(this.session());
            if (header != null) {
                request.header("Accept", header);
            }
            this.mvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith("text/html"));
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"*/*", "text/html;q=0,*/*;q=1"})
    void defaultsRequirementToJson(final String header) throws Exception {
        final MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(
            "/federal/curriculums/1/requirements/7"
        ).session(this.session());
        if (header != null) {
            request.header("Accept", header);
        }
        this.mvc.perform(request).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().contentTypeCompatibleWith(
                "application/vnd.surok.federal-curriculum-requirement-v1+json"
            )
        ).andExpect(MockMvcResultMatchers.jsonPath("$.id").value(7));
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/json", "application/xml", "*/*;q=0", "invalid"})
    void rejectsUnsupportedRead(final String header) throws Exception {
        for (final String path : List.of("", "/1", "/1/requirements", "/1/requirements/7")) {
            this.mvc.perform(
                MockMvcRequestBuilders.get(String.format("/federal/curriculums%s", path))
                    .session(this.session()).header("Accept", header)
            ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
        }
    }

    @ParameterizedTest
    @CsvSource({
        "/create, federal-curriculum-v1, json",
        "/create, federal-curriculum-v2, xml",
        "/1/edit, federal-curriculum-v2, json",
        "/1/edit, federal-curriculum-v1, xml",
        "/1/requirements, federal-curriculum-requirement-v1, json",
        "/1/requirements, federal-curriculum-requirement-v1, xml",
        "/1/requirements/7, federal-curriculum-requirement-v1, json",
        "/1/requirements/7, federal-curriculum-requirement-v1, xml"
    })
    void negotiatesBeforeMutation(
        final String path, final String resource, final String format
    ) throws Exception {
        final String expected = String.format("application/vnd.surok.%s+%s", resource, format);
        final int status;
        if (path.endsWith("/create") || path.endsWith("/requirements")) {
            status = 201;
        } else {
            status = 200;
        }
        final ResultActions result = this.mvc.perform(
            this.authorized(path).header(
                "Accept", String.format("text/html;q=0.1,%s;q=0.9", expected)
            )
        ).andExpect(MockMvcResultMatchers.status().is(status))
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(expected))
            .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("After")));
        if (status == 201) {
            result.andExpect(
                MockMvcResultMatchers.header().string(
                    "Location",
                    Matchers.matchesPattern("/federal/curriculums/[0-9]+(/requirements/[0-9]+)?")
                )
            );
        } else {
            result.andExpect(MockMvcResultMatchers.header().doesNotExist("Location"));
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = {"application/json", "application/xml", "*/*;q=0", "invalid", "application/pdf"}
    )
    void rejectsUnsupportedAcceptBeforeMutation(final String header) throws Exception {
        for (final String path : List.of(
            "/create", "/1/edit", "/1/requirements", "/1/requirements/7"
        )) {
            this.mvc.perform(this.authorized(path).header("Accept", header))
                .andExpect(MockMvcResultMatchers.status().isNotAcceptable());
        }
        MatcherAssert.assertThat(
            this.ctx.resultQuery(
                """
                    SELECT title AS value FROM public.federal_curriculum
                    UNION ALL
                    SELECT subject.name AS value FROM public.federal_curriculum_requirement
                    JOIN public.subject ON subject.id = federal_curriculum_requirement.subject_id
                    """
            ).fetch().getValues("value", String.class),
            Matchers.containsInAnyOrder("Basic plan", "Mathematics")
        );
    }

    @ParameterizedTest
    @CsvSource({
        "/create, /federal/curriculums/[0-9]+",
        "/1/edit, /federal/curriculums/1",
        "/1/requirements, /federal/curriculums/1/requirements",
        "/1/requirements/7, /federal/curriculums/1/requirements"
    })
    void redirectsHtmlMutation(final String path, final String location) throws Exception {
        this.mvc.perform(this.authorized(path).accept(MediaType.TEXT_HTML))
            .andExpect(MockMvcResultMatchers.status().isFound()).andExpect(
                MockMvcResultMatchers.header().string("Location", Matchers.matchesPattern(location))
            ).andExpect(MockMvcResultMatchers.content().string(""));
    }

    @ParameterizedTest
    @CsvSource({
        "/1, federal_curriculum, 1",
        "/1/requirements/7, federal_curriculum_requirement, 7"
    })
    void removesWithoutResponseBody(
        final String path, final String table, final long identifier
    ) throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.delete(String.format("/federal/curriculums%s", path))
            )
        ).andExpect(MockMvcResultMatchers.status().isNoContent())
            .andExpect(MockMvcResultMatchers.content().string(""));
        MatcherAssert.assertThat(
            this.ctx.resultQuery(
                String.format("SELECT is_deleted FROM public.%s WHERE id = ?", table), identifier
            ).fetchOneInto(Boolean.class),
            Matchers.is(true)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/fragment", "/1", "/1/requirements", "/1/requirements/fragment"})
    void letsReadersBrowseTheStandard(final String path) throws Exception {
        MatcherAssert.assertThat(
            "An authenticated reader could not browse the federal standard",
            this.mvc.perform(
                MockMvcRequestBuilders.get(String.format("/federal/curriculums%s", path))
                    .session(this.session(Roles.RoleEnum.VIEWER)).accept(MediaType.TEXT_HTML)
            ).andReturn().getResponse().getStatus(),
            Matchers.is(200)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"/create", "/1/edit"})
    void hidesProtectedFormsFromReaders(final String path) throws Exception {
        MatcherAssert.assertThat(
            "A reader could open an administrative curriculum form",
            this.mvc.perform(
                MockMvcRequestBuilders.get(String.format("/federal/curriculums%s", path))
                    .session(this.session(Roles.RoleEnum.VIEWER)).accept(MediaType.TEXT_HTML)
            ).andReturn().getResponse().getStatus(),
            Matchers.is(403)
        );
    }

    @ParameterizedTest
    @ValueSource(
        strings = {"/create", "/1/edit", "/1/requirements", "/1/requirements/7"}
    )
    void rejectsReaderMutationsWithValidCsrf(final String path) throws Exception {
        MatcherAssert.assertThat(
            "A reader changed a federal resource despite valid CSRF protection",
            this.mvc.perform(
                this.authorized(path, Roles.RoleEnum.VIEWER).accept(MediaType.TEXT_HTML)
            ).andReturn().getResponse().getStatus(),
            Matchers.is(403)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "/1/requirements, 999999",
        "/1/requirements/7, 999999",
        "/1/requirements, 103",
        "/1/requirements/7, 103",
        "/1/requirements, 107",
        "/1/requirements/7, 107"
    })
    void rejectsUnavailableOrSchoolSubjects(final String path, final String subject)
        throws Exception {
        this.ctx.execute(
            "INSERT INTO school (id, name) VALUES (97, 'Лицей') ON CONFLICT DO NOTHING"
        );
        this.ctx.execute(
            """
            INSERT INTO subject (id, school_id, origin, name, is_deleted)
            VALUES (103, NULL, 'FEDERAL', 'Удалённый предмет', true),
                (107, 97, 'SCHOOL', 'Робототехника', false) ON CONFLICT DO NOTHING
            """
        );
        MatcherAssert.assertThat(
            "A requirement accepted an unavailable or school subject",
            this.mvc.perform(
                this.authorized(path, Roles.RoleEnum.ADMIN).param("subjectId", subject)
                    .accept("application/vnd.surok.federal-curriculum-requirement-v1+json")
            ).andReturn().getResponse().getStatus(),
            Matchers.is(400)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"/1", "/1/requirements/7"})
    void rejectsReaderRemovalWithValidCsrf(final String path) throws Exception {
        MatcherAssert.assertThat(
            "A reader deleted a federal plan or requirement",
            this.mvc.perform(
                this.authorized(path, Roles.RoleEnum.VIEWER, HttpMethod.DELETE)
                    .accept(MediaType.TEXT_HTML)
            ).andReturn().getResponse().getStatus(),
            Matchers.is(403)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "'', federal-curriculums.v1", "/1, federal-curriculum-v1",
        "/1/requirements, federal-curriculum-requirements.v1",
        "/1/requirements/7, federal-curriculum-requirement-v1"
    })
    void letsReadersRetrieveVersionedResources(final String path, final String resource)
        throws Exception {
        MatcherAssert.assertThat(
            "An authenticated reader could not retrieve a versioned federal representation",
            this.mvc.perform(
                MockMvcRequestBuilders.get(String.format("/federal/curriculums%s", path))
                    .session(this.session(Roles.RoleEnum.VIEWER))
                    .accept(String.format("application/vnd.surok.%s+json", resource))
            ).andReturn().getResponse().getStatus(),
            Matchers.is(200)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "PRIMARY, 1, true", "PRIMARY, 4, true", "PRIMARY, 5, false",
        "PRIMARY, 10, false", "BASIC, 4, false", "BASIC, 5, true",
        "BASIC, 9, true", "BASIC, 10, false", "SECONDARY, 4, false",
        "SECONDARY, 9, false", "SECONDARY, 10, true", "SECONDARY, 11, true"
    })
    void validatesRequirementGradesAgainstThePlanLevel(
        final String level, final String grade, final boolean valid
    ) throws Exception {
        this.ctx.execute(
            "UPDATE federal_curriculum SET education_level = ?::education_level_type WHERE id = 1",
            level
        );
        final List<Integer> statuses = new ArrayList<>(2);
        for (final String action : List.of("", "/7")) {
            statuses.add(
                this.mvc.perform(
                    this.authorized(
                        MockMvcRequestBuilders.post(
                            String.format("/federal/curriculums/1/requirements%s", action)
                        )
                    ).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grade", grade).param("hours", "3").param(
                            "subjectId", Map.of("", "516", "/7", "504").get(action)
                        )
                        .param("partType", "MANDATORY")
                        .accept("application/vnd.surok.federal-curriculum-requirement-v1+json")
                ).andReturn().getResponse().getStatus()
            );
        }
        final List<Integer> expected;
        if (valid) {
            expected = List.of(201, 200);
        } else {
            expected = List.of(400, 400);
        }
        MatcherAssert.assertThat(
            "The API accepted an inapplicable grade or rejected a grade of the plan level",
            statuses,
            Matchers.is(expected)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"create", "7/edit"})
    void preservesRequirementsWhenTheGradeIsOutsideThePlan(final String action) throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post(
                    String.format("/federal/curriculums/1/requirements%s", action)
                )
            ).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grade", "3").param("hours", "7")
                .param("subjectId", "516").param("partType", "MANDATORY")
                .accept("application/vnd.surok.federal-curriculum-requirement-v1+json")
        );
        MatcherAssert.assertThat(
            "An invalid grade changed the saved requirements",
            this.ctx.fetch(
                """
                SELECT grade, subject.name AS subject_name, hours
                FROM federal_curriculum_requirement
                JOIN subject ON subject.id = federal_curriculum_requirement.subject_id
                """
            ).intoMaps(),
            Matchers.is(
                List.of(Map.of("grade", 5, "subject_name", "Mathematics", "hours", 5))
            )
        );
    }

    @ParameterizedTest
    @CsvSource({
        "PRIMARY, 1 2 3 4, ''", "BASIC, 5 6 7 8 9, ''", "SECONDARY, 10 11, ''",
        "PRIMARY, 1 2 3 4, /fragment", "BASIC, 5 6 7 8 9, /fragment",
        "SECONDARY, 10 11, /fragment"
    })
    void offersOnlyGradesOfThePlanInRequirementForms(
        final String level, final String grades, final String suffix
    ) throws Exception {
        this.ctx.execute(
            "UPDATE federal_curriculum SET education_level = ?::education_level_type WHERE id = 1",
            level
        );
        final List<String> expected = new ArrayList<>(2);
        expected.add(grades);
        if (suffix.isEmpty()) {
            expected.add(grades);
        }
        MatcherAssert.assertThat(
            "A requirement form offered grades outside the plan level",
            Pattern.compile(
                "<select[^>]*(?:grade-number|requirement-grade-input)\"[^>]*>(.*?)</select>",
                Pattern.DOTALL
            ).matcher(
                this.mvc.perform(
                    MockMvcRequestBuilders.get(
                        String.format("/federal/curriculums/1/requirements%s", suffix)
                    ).session(this.session()).accept(MediaType.TEXT_HTML)
                ).andReturn().getResponse().getContentAsString()
            ).results().map(
                select -> Pattern.compile("<option[^>]*value=\"([0-9]+)\"")
                    .matcher(select.group(1)).results().map(option -> option.group(1)).toList()
            ).map(values -> String.join(" ", values)).toList(),
            Matchers.is(expected)
        );
    }

    private MockHttpServletRequestBuilder authorized(final String path, final Roles.RoleEnum role)
        throws Exception {
        return this.authorized(path, role, HttpMethod.POST);
    }

    private MockHttpServletRequestBuilder authorized(
        final String path, final Roles.RoleEnum role, final HttpMethod method
    ) throws Exception {
        final MockHttpSession session = this.session(role);
        final MvcResult page = this.mvc.perform(
            MockMvcRequestBuilders.get("/federal/subjects").session(session)
        ).andReturn();
        final CsrfToken token = (CsrfToken) page.getRequest().getAttribute(
            CsrfToken.class.getName()
        );
        return MockMvcRequestBuilders.request(method, String.format("/federal/curriculums%s", path))
            .session(session).cookie(page.getResponse().getCookies())
            .header(token.getHeaderName(), token.getToken())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("grade", "6")
            .param("hours", "2").param("partType", "MANDATORY");
    }

    private MockHttpServletRequestBuilder authorized(final String path) throws Exception {
        return this.authorized(
            MockMvcRequestBuilders.post(String.format("/federal/curriculums%s", path))
        ).contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("title", "After").param("level", "BASIC").param("week", "FIVE_DAYS")
            .param("version", "2").param("year", "2026/2027").param("description", "Description")
            .param("grade", "6").param("subjectId", "516").param("hours", "2")
            .param("partType", "MANDATORY");
    }

    private MockHttpServletRequestBuilder authorized(final MockHttpServletRequestBuilder request)
        throws Exception {
        final MockHttpSession session = this.session();
        final MvcResult form = this.mvc.perform(
            MockMvcRequestBuilders.get("/federal/curriculums/create").session(session)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        final CsrfToken token = (CsrfToken) form.getRequest().getAttribute(
            CsrfToken.class.getName()
        );
        return request.session(session).cookie(form.getResponse().getCookies())
            .header(token.getHeaderName(), token.getToken());
    }

    private MockHttpSession session() throws Exception {
        return this.session(Roles.RoleEnum.ADMIN);
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
        registry.add("spring.datasource.url", CrsHttpTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", CrsHttpTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", CrsHttpTest.POSTGRES::getPassword);
    }
}
