/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schoolclass;

import com.eshabakhov.surok.User;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.school.schedule.load.ClassLoadPostgres;
import com.eshabakhov.surok.user.AuthUser;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrPostgres;
import com.eshabakhov.surok.user.UrsPostgres;
import java.util.List;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * School class HTTP and integration contracts verified with MockMvc assertions.
 *
 * @since 0.0.1
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
final class ScsHttpTest {

    /** Postgres container. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("surok")
            .withUsername("test")
            .withPassword("test");

    /** Database context. */
    private final DSLContext ctx;

    /** HTTP client with the application's security filters. */
    private final MockMvc mvc;

    @Autowired
    ScsHttpTest(final DSLContext ctx, final MockMvc mvc) {
        this.ctx = ctx;
        this.mvc = mvc;
    }

    @BeforeEach
    void insertSchoolClasses() {
        this.ctx.resultQuery(
            """
                INSERT INTO public.school (id, name)
                VALUES (1, 'First school'), (2, 'Second school')
                """
        ).execute();
        this.ctx.resultQuery(
            """
                INSERT INTO public.school_class (id, school_id, grade, litera, is_deleted)
                VALUES (10, 1, 2, 'А', false), (11, 1, 10, 'Б', false),
                    (12, 1, 2, 'Удалён', true), (20, 2, 2, 'Чужой', false)
                """
        ).execute();
    }

    @AfterEach
    void truncateSchools() {
        this.ctx.resultQuery(
            """
                TRUNCATE TABLE public.school, subject RESTART IDENTITY CASCADE
                """
        ).execute();
    }

    @Test
    void requiresAuthentication() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/classes")
        ).andExpect(MockMvcResultMatchers.status().isFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"name", "name_ct", "filter"})
    void scopesFilteredPages(final String parameter) throws Exception {
        String value = "2";
        if ("filter".equals(parameter)) {
            value = "name:2";
        }
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/classes")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L))
                .accept("application/vnd.surok.school-classes.v1+json")
                .param(parameter, value).param("limit", "1")
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.items.length()").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[0].name").value("2А"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.page.total").value(1));
    }

    @ParameterizedTest
    @CsvSource({"ASC, 2А, 10Б", "DESC, 10Б, 2А", "NONE, 2А, 10Б"})
    void sortsGradesNumerically(final String direction, final String first, final String second)
        throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/classes")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L))
                .accept("application/vnd.surok.school-classes.v1+json")
                .param("sort", String.format("name:%s", direction))
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[0].name").value(first))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[1].name").value(second));
    }

    @Test
    void printsVersionedEntity() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/classes/10")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L))
                .accept("application/vnd.surok.school-class-v1+json")
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().json(
                "{\"id\":10,\"name\":\"2А\",\"grade\":2,\"litera\":\"А\"}"
            )
        );
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/classes/10")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L))
                .accept("application/vnd.surok.school-class-v1+xml")
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.xpath("/school-class/grade").string("2"))
            .andExpect(MockMvcResultMatchers.xpath("/school-class/litera").string("А"));
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/classes")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L))
                .accept("application/vnd.surok.school-classes.v1+xml")
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.xpath("count(/classes/school-class)").number(2.0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/create", "/10", "/10/edit"})
    void rendersXsltPages(final String suffix) throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get(String.format("/schools/1/classes%s", suffix))
                .session(this.session(Roles.RoleEnum.VIEWER, 1L))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().string(Matchers.not(Matchers.emptyString()))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/json", "application/xml", "application/pdf", "*/*;q=0"})
    void rejectsUnsupportedRepresentations(final String accept) throws Exception {
        for (final String suffix : List.of("", "/create", "/10", "/10/edit")) {
            this.mvc.perform(
                MockMvcRequestBuilders.get(String.format("/schools/1/classes%s", suffix))
                    .session(this.session(Roles.RoleEnum.VIEWER, 1L)).header("Accept", accept)
            ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
        }
    }

    /** Verifies the collection accepts versioned JSON for creation. */
    @Test
    void createsVersionedJson() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/classes"),
                Roles.RoleEnum.DIRECTOR
            ).contentType("application/vnd.surok.school-class-v1+json")
                .accept("application/vnd.surok.school-class-v1+json")
                .content("{\"grade\":5,\"litera\":\" В \"}")
        ).andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("5В"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.grade").value(5)).andExpect(
                MockMvcResultMatchers.header().string(
                    "Location", Matchers.startsWith("/schools/1/classes/")
                )
            );
    }

    /** Verifies both fields can be updated using the edit page's CSRF token. */
    @Test
    void revisesBothFieldsThroughTheEditPagesCsrfHeader() throws Exception {
        final MockHttpSession session = this.session(Roles.RoleEnum.DIRECTOR, 1L);
        final MvcResult form = this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/classes/10/edit").session(session)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        final CsrfToken token = (CsrfToken) form.getRequest().getAttribute(
            CsrfToken.class.getName()
        );
        this.mvc.perform(
            MockMvcRequestBuilders.put("/schools/1/classes/10").session(session)
                .cookie(form.getResponse().getCookies())
                .header(token.getHeaderName(), token.getToken())
                .contentType("application/vnd.surok.school-class-v1+json")
                .accept("application/vnd.surok.school-class-v1+json")
                .content("{\"grade\":3,\"litera\":\" В \"}")
        ).andExpect(MockMvcResultMatchers.status().isOk());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/classes/10")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L))
                .accept("application/vnd.surok.school-class-v1+json")
        ).andExpect(MockMvcResultMatchers.jsonPath("$.name").value("3В"));
    }

    @Test
    void rejectsDuplicateRevisionsAtomically() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.put("/schools/1/classes/10"), Roles.RoleEnum.DIRECTOR
            ).contentType("application/vnd.surok.school-class-v1+json")
                .accept("application/vnd.surok.school-class-v1+json")
                .content("{\"grade\":10,\"litera\":\"Б\"}")
        ).andExpect(MockMvcResultMatchers.status().isConflict());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/classes/10")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L))
                .accept("application/vnd.surok.school-class-v1+json")
        ).andExpect(MockMvcResultMatchers.jsonPath("$.name").value("2А"));
    }

    @Test
    void rejectsDuplicateCreation() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/classes"), Roles.RoleEnum.DIRECTOR
            ).contentType("application/vnd.surok.school-class-v1+json")
                .accept("application/vnd.surok.school-class-v1+json")
                .content("{\"grade\":2,\"litera\":\"А\"}")
        ).andExpect(MockMvcResultMatchers.status().isConflict());
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "{}", "{\"grade\":5}", "{\"grade\":5.5,\"litera\":\"А\"}",
            "{\"grade\":0,\"litera\":\"А\"}", "{\"grade\":12,\"litera\":\"А\"}",
            "{\"grade\":5,\"litera\":\" \"}", "{\"grade\":\"bad\",\"litera\":\"А\"}"
        }
    )
    void rejectsInvalidJson(final String json) throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.put("/schools/1/classes/10"), Roles.RoleEnum.DIRECTOR
            ).contentType("application/vnd.surok.school-class-v1+json")
                .accept("application/vnd.surok.school-class-v1+json").content(json)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/classes/10")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L))
                .accept("application/vnd.surok.school-class-v1+json")
        ).andExpect(MockMvcResultMatchers.jsonPath("$.name").value("2А"));
    }

    /** Verifies invalid form input returns to the creation page. */
    @Test
    void redirectsInvalidClassCreation() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/classes"), Roles.RoleEnum.DIRECTOR
            ).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grade", "bad").param("litera", "А")
        ).andExpect(MockMvcResultMatchers.status().isFound()).andExpect(
            MockMvcResultMatchers.redirectedUrl("/schools/1/classes/create?error=empty")
        );
    }

    /** Verifies the native form creates a class at the canonical collection URL. */
    @Test
    void createsAClassThroughTheProtectedForm() throws Exception {
        final MockHttpSession session = this.session(Roles.RoleEnum.DIRECTOR, 1L);
        final MvcResult form = this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/classes/create").session(session)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        final CsrfToken token = (CsrfToken) form.getRequest().getAttribute(
            CsrfToken.class.getName()
        );
        this.mvc.perform(
            MockMvcRequestBuilders.post("/schools/1/classes").session(session)
                .cookie(form.getResponse().getCookies())
                .param(token.getParameterName(), token.getToken())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grade", "4").param("litera", "Г")
        ).andExpect(MockMvcResultMatchers.redirectedUrl("/schools/1/classes"));
    }

    /**
     * Verifies removed creation and update aliases reject requests.
     *
     * @param suffix Removed route suffix
     * @param type Former request format
     */
    @ParameterizedTest
    @CsvSource({
        "/, application/vnd.surok.school-class-v1+json",
        "/create, application/vnd.surok.school-class-v1+json",
        "/create, application/x-www-form-urlencoded",
        "/10/edit, application/x-www-form-urlencoded"
    })
    void rejectsLegacyClassMutationRoutes(final String suffix, final String type) throws Exception {
        final MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(
            String.format("/schools/1/classes%s", suffix)
        ).contentType(type);
        if (MediaType.APPLICATION_FORM_URLENCODED_VALUE.equals(type)) {
            request.param("grade", "5").param("litera", "Д");
        } else {
            request.content("{\"grade\":5,\"litera\":\"Д\"}");
        }
        this.mvc.perform(this.authorized(request, Roles.RoleEnum.DIRECTOR))
            .andExpect(MockMvcResultMatchers.status().is4xxClientError());
    }

    /**
     * Verifies the old list aliases no longer expose a collection.
     *
     * @param suffix Removed route suffix
     */
    @ParameterizedTest
    @ValueSource(strings = {"/", "/fragment"})
    void rejectsLegacyClassListRoutes(final String suffix) throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get(String.format("/schools/1/classes%s", suffix))
                .session(this.session(Roles.RoleEnum.VIEWER, 1L)).accept(MediaType.TEXT_HTML)
        ).andExpect(MockMvcResultMatchers.status().is4xxClientError());
    }

    /** Verifies the creation route exposes the HTML form. */
    @Test
    void rejectsCollectionJsonAtTheClassCreationForm() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/classes/create")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.school-classes.v1+json")
        ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
    }

    /** Verifies the fragment keeps filtering and omits the surrounding page. */
    @Test
    void rendersFilteredClassResultsAsAFragment() throws Exception {
        final String type = "text/vnd.surok.school-classes-fragment+html";
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/classes")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L)).accept(type)
                .param("filter", "name:2А").param("limit", "1")
        ).andExpectAll(
            MockMvcResultMatchers.status().isOk(),
            MockMvcResultMatchers.content().contentTypeCompatibleWith(type),
            MockMvcResultMatchers.header().stringValues(
                HttpHeaders.VARY, Matchers.hasItem(HttpHeaders.ACCEPT)
            ),
            MockMvcResultMatchers.content().string(
                Matchers.allOf(
                    Matchers.containsString("id=\"school-classes-results\""),
                    Matchers.containsString("2А"),
                    Matchers.not(Matchers.containsString("10Б")),
                    Matchers.not(Matchers.containsString("<html"))
                )
            )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/json", "application/vnd.surok.school-classes.v1+json"})
    void rejectsUnsupportedInput(final String content) throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/classes"), Roles.RoleEnum.DIRECTOR
            ).contentType(content).accept("application/vnd.surok.school-class-v1+json")
                .content("{\"grade\":3,\"litera\":\"А\"}")
        ).andExpect(MockMvcResultMatchers.status().isUnsupportedMediaType());
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/json", "invalid", "application/pdf", "*/*;q=0"})
    void checksAcceptBeforeMutations(final String accept) throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/classes"), Roles.RoleEnum.DIRECTOR
            ).contentType("application/vnd.surok.school-class-v1+json").header("Accept", accept)
                .content("{\"grade\":3,\"litera\":\"А\"}")
        ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.put("/schools/1/classes/10"), Roles.RoleEnum.DIRECTOR
            ).contentType("application/vnd.surok.school-class-v1+json").header("Accept", accept)
                .content("{\"grade\":4,\"litera\":\"А\"}")
        ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/classes"), Roles.RoleEnum.DIRECTOR
            ).contentType(MediaType.APPLICATION_FORM_URLENCODED).header("Accept", accept)
                .param("grade", "6").param("litera", "А")
        ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/classes")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L))
                .accept("application/vnd.surok.school-classes.v1+json")
        ).andExpect(MockMvcResultMatchers.jsonPath("$.page.total").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[0].name").value("2А"));
    }

    @ParameterizedTest
    @ValueSource(longs = {12L, 20L, 999L})
    void rejectsUnavailableClasses(final long identifier) throws Exception {
        final String path = String.format("/schools/1/classes/%d", identifier);
        this.mvc.perform(
            MockMvcRequestBuilders.get(path).session(this.session(Roles.RoleEnum.VIEWER, 1L))
        ).andExpect(MockMvcResultMatchers.status().isNotFound());
        this.mvc.perform(
            this.authorized(MockMvcRequestBuilders.put(path), Roles.RoleEnum.DIRECTOR)
                .contentType("application/vnd.surok.school-class-v1+json")
                .content("{\"grade\":8,\"litera\":\"А\"}")
        ).andExpect(MockMvcResultMatchers.status().isNotFound());
        this.mvc.perform(
            this.authorized(MockMvcRequestBuilders.delete(path), Roles.RoleEnum.DIRECTOR)
        ).andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void deniesViewerMutationsAndForeignSchools() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/classes"), Roles.RoleEnum.VIEWER
            ).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grade", "3").param("litera", "А")
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/2/classes/20")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void deniesClassCreationToSubscribedMemberWithoutSchoolRole() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/classes"),
                Roles.RoleEnum.BASIC_MAKER
            ).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grade", "7").param("litera", "Я")
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void allowsDeputyDirectorToCreateAClass() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/classes"),
                Roles.RoleEnum.DEPUTY_DIRECTOR
            ).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grade", "7").param("litera", "Я")
        ).andExpect(MockMvcResultMatchers.status().isFound());
    }

    @Test
    void protectsMutationsWithCsrf() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.post("/schools/1/classes")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grade", "3").param("litera", "А")
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.delete("/schools/1/classes/10"), Roles.RoleEnum.DIRECTOR
            )
        ).andExpect(MockMvcResultMatchers.status().isNoContent());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/classes/10")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L))
        ).andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void keepsClassLoadIntegration() throws Exception {
        this.insertLoad();
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules/100/loads")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().string(
                Matchers.allOf(
                    Matchers.containsString("2А"),
                    Matchers.not(Matchers.containsString("10Б")),
                    Matchers.containsString("Математика"),
                    Matchers.not(Matchers.containsString("Удалён")),
                    Matchers.not(Matchers.containsString("Чужой"))
                )
            )
        );
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules/100")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().string(
                Matchers.containsString("class=\"stat-value\">2</div>")
            )
        );
        MatcherAssert.assertThat(
            new ClassLoadPostgres(this.ctx, 40L)
                .print(new MdJson()).print().path("schoolClass").path("name").asText(),
            Matchers.equalTo("2А")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/10", "/fragment"})
    void removesOldApiAliases(final String suffix) throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get(String.format("/api/schools/1/classes%s", suffix))
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
        ).andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void retainsLoadsOfRemovedClasses() throws Exception {
        this.insertLoad();
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.delete("/schools/1/classes/10"), Roles.RoleEnum.DIRECTOR
            )
        ).andExpect(MockMvcResultMatchers.status().isNoContent());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules/100/loads")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().string(
                Matchers.allOf(
                    Matchers.containsString(">2А</a>"),
                    Matchers.not(Matchers.containsString(">2А</option>"))
                )
            )
        );
        MatcherAssert.assertThat(
            new ClassLoadPostgres(this.ctx, 40L).print(new MdJson()).print()
                .path("schoolClass").path("name").asText(),
            Matchers.equalTo("2А")
        );
    }

    private void insertLoad() {
        this.ctx.resultQuery(
            """
            INSERT INTO public.schedule (id, school_id, name) VALUES (100, 1, 'Год')
            """
        ).execute();
        this.ctx.resultQuery(
            """
            INSERT INTO subject (id, school_id, origin, name)
        VALUES (30, 1, 'SCHOOL', 'Математика')
            """
        ).execute();
        this.ctx.resultQuery(
            """
            INSERT INTO public.class_curriculum
                (id, schedule_id, school_class_id, subject_id, hours)
            VALUES (40, 100, 10, 30, 5)
            """
        ).execute();
    }

    private MockHttpServletRequestBuilder authorized(
        final MockHttpServletRequestBuilder request, final Roles.RoleEnum role
    ) throws Exception {
        final MockHttpSession session = this.session(role, 1L);
        final MvcResult form = this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/classes/create").session(session)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        final CsrfToken token = (CsrfToken) form.getRequest().getAttribute(
            CsrfToken.class.getName()
        );
        return request.session(session).cookie(form.getResponse().getCookies())
            .header(token.getHeaderName(), token.getToken());
    }

    private MockHttpSession session(final Roles.RoleEnum role, final Long school)
        throws Exception {
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
        if (school != null) {
            new UrsPostgres(this.ctx, school).add(user);
        }
        if (List.of(
            Roles.RoleEnum.DIRECTOR, Roles.RoleEnum.DEPUTY_DIRECTOR,
            Roles.RoleEnum.TEACHER, Roles.RoleEnum.STUDENT, Roles.RoleEnum.VIEWER
        ).contains(role)) {
            new UrsPostgres(this.ctx, school).roles(user).grant(role);
        } else {
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
        registry.add("spring.datasource.url", ScsHttpTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", ScsHttpTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", ScsHttpTest.POSTGRES::getPassword);
    }
}
