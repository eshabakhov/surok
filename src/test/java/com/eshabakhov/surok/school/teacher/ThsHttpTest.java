/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.teacher;

import com.eshabakhov.surok.User;
import com.eshabakhov.surok.user.AuthUser;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrPostgres;
import com.eshabakhov.surok.user.UrsPostgres;
import java.util.List;
import java.util.UUID;
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
 * Existing teacher HTTP contracts verified with MockMvc assertions.
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
final class ThsHttpTest {

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
    ThsHttpTest(final DSLContext ctx, final MockMvc mvc) {
        this.ctx = ctx;
        this.mvc = mvc;
    }

    @BeforeEach
    void insertSchoolsAndTeachers() {
        this.ctx.resultQuery(
            """
                INSERT INTO public.school (id, name)
                VALUES (1, 'First school'), (2, 'Second school')
                """
        ).execute();
        this.ctx.resultQuery(
            """
                INSERT INTO public.teacher (id, school_id, name, is_deleted)
                VALUES (10, 1, 'North', false), (11, 1, 'South', false),
                    (12, 1, 'North removed', true), (20, 2, 'North foreign', false)
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
            MockMvcRequestBuilders.get("/schools/1/teachers").accept(MediaType.TEXT_HTML)
        ).andExpect(MockMvcResultMatchers.status().isFound())
            .andExpect(MockMvcResultMatchers.redirectedUrl("http://localhost/users/login"));
    }

    @Test
    void printsJsonPage() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/teachers/")
                .accept("application/vnd.surok.teachers.v1+json")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L))
                .param("name_ct", "north").param("limit", "1")
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.items.length()").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[0].name").value("North"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.page.total").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.page.limit").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.page.offset").value(1));
    }

    @Test
    void printsJsonTeacher() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/teachers/10")
                .accept("application/vnd.surok.teacher-v1+json")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L))
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().json("{\"id\":10,\"name\":\"North\"}"));
    }

    @Test
    void deniesAnotherSchool() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/2/teachers/20")
                .accept("application/vnd.surok.teacher-v1+json")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void deniesJsonCreationForViewer() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/teachers"), Roles.RoleEnum.VIEWER
            ).accept("application/vnd.surok.teacher-v1+json")
                .contentType("application/vnd.surok.teacher-v1+json")
                .content("{\"name\":\"East\"}")
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void deniesTeacherCreationToSubscribedMemberWithoutSchoolRole() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/teachers/create"),
                Roles.RoleEnum.BASIC_MAKER
            ).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "Учитель подписчика")
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void allowsDeputyDirectorToCreateATeacher() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/teachers/create"),
                Roles.RoleEnum.DEPUTY_DIRECTOR
            ).contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "Учитель заместителя")
        ).andExpect(MockMvcResultMatchers.status().isFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/", "/create"})
    void createsThroughJsonAliases(final String suffix) throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post(
                    String.format("%s%s", "/schools/1/teachers", suffix)
                ), Roles.RoleEnum.DIRECTOR
            ).contentType("application/vnd.surok.teacher-v1+json")
                .accept("application/vnd.surok.teacher-v1+json").content("{\"name\":\"East\"}")
        ).andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("East"));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "application/json", "application/xml",
            "application/vnd.surok.teacher-v2+json",
            "application/vnd.surok.teachers.v1+json"
        }
    )
    void rejectsUnsupportedInputBeforeMutation(final String type) throws Exception {
        for (final String suffix : List.of("", "/", "/create", "/10/edit")) {
            this.mvc.perform(
                this.authorized(
                    MockMvcRequestBuilders.post(
                        String.format("%s%s", "/schools/1/teachers", suffix)
                    ), Roles.RoleEnum.DIRECTOR
                ).contentType(type).accept("application/vnd.surok.teacher-v1+json")
                    .content("{\"name\":\"Rejected\"}")
            ).andExpect(MockMvcResultMatchers.status().isUnsupportedMediaType());
        }
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.put("/schools/1/teachers/10"),
                Roles.RoleEnum.DIRECTOR
            ).contentType(type).accept("application/vnd.surok.teacher-v1+json")
                .content("{\"name\":\"Rejected\"}")
        ).andExpect(MockMvcResultMatchers.status().isUnsupportedMediaType());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/teachers")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.teachers.v1+json").param("name_ct", "Rejected")
        ).andExpect(MockMvcResultMatchers.jsonPath("$.page.total").value(0));
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/teachers/10")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.teacher-v1+json")
        ).andExpect(MockMvcResultMatchers.jsonPath("$.name").value("North"));
    }

    @Test
    void createsTeacherFromJson() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/teachers"),
                Roles.RoleEnum.DIRECTOR
            ).accept("application/vnd.surok.teacher-v1+json")
                .contentType("application/vnd.surok.teacher-v1+json")
                .content("{\"name\":\"East\"}")
        ).andExpect(MockMvcResultMatchers.status().isCreated()).andExpect(
            MockMvcResultMatchers.header().string(
                "Location", Matchers.startsWith("/schools/1/teachers/")
            )
        ).andExpect(MockMvcResultMatchers.jsonPath("$.name").value("East"));
    }

    @ParameterizedTest
    @ValueSource(longs = {12L, 20L, 999L})
    void rejectsRevisionOfUnavailableTeacher(final long identifier) throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.put("/schools/1/teachers/{id}", identifier),
                Roles.RoleEnum.DIRECTOR
            ).accept("application/vnd.surok.teacher-v1+json")
                .contentType("application/vnd.surok.teacher-v1+json")
                .content("{\"name\":\"West\"}")
        ).andExpect(MockMvcResultMatchers.status().isNotFound());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/teachers")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.teachers.v1+json").param("name_ct", "West")
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.page.total").value(0));
    }

    @Test
    void revisesTeacherFromJson() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.put("/schools/1/teachers/10"), Roles.RoleEnum.DIRECTOR
            ).accept("application/vnd.surok.teacher-v1+json")
                .contentType("application/vnd.surok.teacher-v1+json")
                .content("{\"name\":\"Renamed\"}")
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().json("{\"id\":10,\"name\":\"Renamed\"}")
        );
    }

    @Test
    void rendersTeacherPages() throws Exception {
        for (final String path : List.of(
            "", "/fragment", "/create", "/10", "/10/edit"
        )) {
            this.mvc.perform(
                MockMvcRequestBuilders.get(String.format("/schools/1/teachers%s", path))
                    .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
            ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
                MockMvcResultMatchers.content().string(Matchers.not(Matchers.emptyString()))
            );
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/create", "/10/edit"})
    void redirectsInvalidBrowserForms(final String suffix) throws Exception {
        final String form = String.format("/schools/1/teachers%s", suffix);
        this.mvc.perform(
            this.authorized(MockMvcRequestBuilders.post(form), Roles.RoleEnum.DIRECTOR)
                .accept(MediaType.TEXT_HTML)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", " ")
        ).andExpect(MockMvcResultMatchers.status().isFound()).andExpect(
            MockMvcResultMatchers.redirectedUrl(String.format("%s?error=empty", form))
        );
        this.mvc.perform(
            MockMvcRequestBuilders.get(form).param("error", "empty")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept(MediaType.TEXT_HTML)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().string(
                Matchers.containsString("Имя не может быть пустым")
            )
        );
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/teachers")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.teachers.v1+json")
        ).andExpect(MockMvcResultMatchers.jsonPath("$.page.total").value(2));
    }

    @ParameterizedTest
    @CsvSource({
        "/create, application/vnd.surok.teacher-v1+json",
        "/create, application/vnd.surok.teacher-v1+xml",
        "/10/edit, application/vnd.surok.teacher-v1+json",
        "/10/edit, application/vnd.surok.teacher-v1+xml"
    })
    void keepsApiValidationForFormFields(final String suffix, final String accept)
        throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post(String.format("/schools/1/teachers%s", suffix)),
                Roles.RoleEnum.DIRECTOR
            ).header("Accept", accept)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", " ")
        ).andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.header().doesNotExist("Location"));
    }

    @Test
    void revisesThroughProtectedForm() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/teachers/10/edit"),
                Roles.RoleEnum.DIRECTOR
            ).accept(MediaType.TEXT_HTML)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", " Revised through form ")
        ).andExpect(MockMvcResultMatchers.status().isFound())
            .andExpect(MockMvcResultMatchers.redirectedUrl("/schools/1/teachers/10"));
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/teachers/10")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.teacher-v1+json")
        ).andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Revised through form"));
    }

    @Test
    void createsThroughProtectedForm() throws Exception {
        final MockHttpSession session = this.session(Roles.RoleEnum.DIRECTOR, 1L);
        final MvcResult form = this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/teachers/create").session(session)
        ).andReturn();
        final CsrfToken token = (CsrfToken) form.getRequest().getAttribute(
            CsrfToken.class.getName()
        );
        this.mvc.perform(
            MockMvcRequestBuilders.post("/schools/1/teachers/create")
                .session(session).cookie(form.getResponse().getCookies())
                .param(token.getParameterName(), token.getToken())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", " Web teacher ")
        ).andExpect(MockMvcResultMatchers.status().isFound())
            .andExpect(MockMvcResultMatchers.redirectedUrl("/schools/1/teachers"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/10"})
    void keepsSchoolSchedulesInNavigation(final String suffix) throws Exception {
        this.ctx.resultQuery(
            """
                INSERT INTO public.schedule (id, school_id, name, is_deleted)
                VALUES (100, 1, 'Учебный год', false), (200, 2, 'Чужое расписание', false)
                """
        ).execute();
        this.mvc.perform(
            MockMvcRequestBuilders.get(String.format("/schools/1/teachers%s", suffix))
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept(MediaType.TEXT_HTML)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().string(
                Matchers.allOf(
                    Matchers.containsString("href=\"/schools/1/schedules/100\""),
                    Matchers.containsString("title=\"Учебный год\""),
                    Matchers.not(Matchers.containsString("Чужое расписание"))
                )
            )
        );
    }

    @Test
    void negotiatesJsonOnBrowserRoute() throws Exception {
        final String accept = "application/vnd.surok.teacher-v1+json";
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/teachers/10")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L)).header("Accept", accept)
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(accept))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("North"));
    }

    @Test
    void negotiatesXmlOnBrowserRoute() throws Exception {
        final String accept = "application/vnd.surok.teacher-v1+xml";
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/teachers/10")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L)).header("Accept", accept)
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(accept))
            .andExpect(MockMvcResultMatchers.xpath("/teacher/name").string("North"));
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|', value = {
            "text/html;q=0.2,application/*;q=0.9|application/vnd.surok.teacher-v1+json",
            "text/html;q=0,*/*;q=1|application/vnd.surok.teacher-v1+json",
            "application/xml;q=0.1,*/*;q=0.8|text/html",
            "application/vnd.surok.teacher-v1+xml|application/vnd.surok.teacher-v1+xml",
            "text/html;charset=UTF-8|text/html",
            "*/*|text/html"
        }
    )
    void honorsAcceptPriorities(final String accept, final String expected) throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/teachers/10")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L)).header("Accept", accept)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().contentTypeCompatibleWith(expected)
        );
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "application/pdf", "*/*;q=0", "invalid", "application/json;q=2",
            "application/com.eshabakhov.surok.school.teacher.simpleteacher+json",
            "application/vnd.surok.teachers.v1+json"
        }
    )
    void rejectsUnavailableRepresentations(final String accept) throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/teachers/10")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L)).header("Accept", accept)
        ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/json", "application/xml"})
    void rejectsUnversionedRepresentations(final String accept) throws Exception {
        for (final String suffix : List.of("", "/", "/fragment", "/create", "/10", "/10/edit")) {
            this.mvc.perform(
                MockMvcRequestBuilders.get(String.format("%s%s", "/schools/1/teachers", suffix))
                    .session(this.session(Roles.RoleEnum.VIEWER, 1L)).header("Accept", accept)
            ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
        }
    }

    @ParameterizedTest
    @CsvSource({"ASC, North, South", "DESC, South, North"})
    void sortsByName(final String direction, final String first, final String second)
        throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/teachers")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L))
                .accept("application/vnd.surok.teachers.v1+json")
                .param("sort", String.format("name:%s", direction))
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[0].name").value(first))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[1].name").value(second));
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/teachers/fragment")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L)).accept(MediaType.TEXT_HTML)
                .param("sort", String.format("name:%s", direction))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().string(
                Matchers.containsString("class=\"teachers-sort active\"")
            )
        );
    }

    @Test
    void printsVersionedCollection() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/teachers")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L))
                .header("Accept", "application/vnd.surok.teachers.v1+json")
                .param("filter", "name:North").param("limit", "1")
                .param("sort", "name:DESC")
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.items.length()").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.page.limit").value(1));
    }

    @Test
    void rendersXsltWithNavigationAndToken() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/teachers/create")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .header("Accept", "text/html,application/xhtml+xml;q=0.9,*/*;q=0.8")
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().contentTypeCompatibleWith("text/html")
        ).andExpect(
            MockMvcResultMatchers.content().string(Matchers.containsString("name=\"_csrf\""))
        ).andExpect(
            MockMvcResultMatchers.content().string(
                Matchers.containsString("href=\"/schools/1/teachers\"")
            )
        );
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "application/pdf", "*/*;q=0", "invalid", "application/json;q=2",
            "application/json", "application/xml"
        }
    )
    void rejectsUnsupportedRepresentationBeforeCreation(final String accept) throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/teachers"),
                Roles.RoleEnum.DIRECTOR
            ).header("Accept", accept).contentType("application/vnd.surok.teacher-v1+json")
                .content("{\"name\":\"Rejected\"}")
        ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/teachers")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.teachers.v1+json").param("name_ct", "Rejected")
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.page.total").value(0));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "application/pdf", "*/*;q=0", "invalid", "application/json;q=2",
            "application/json", "application/xml"
        }
    )
    void rejectsUnsupportedRepresentationBeforeFormCreation(final String accept) throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/teachers/create"),
                Roles.RoleEnum.DIRECTOR
            ).header("Accept", accept)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "Rejected")
        ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/teachers")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.teachers.v1+json").param("name_ct", "Rejected")
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.page.total").value(0));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "application/pdf", "*/*;q=0", "invalid", "application/json;q=2",
            "application/json", "application/xml"
        }
    )
    void rejectsUnsupportedRepresentationBeforeRevision(final String accept) throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.put("/schools/1/teachers/10"),
                Roles.RoleEnum.DIRECTOR
            ).header("Accept", accept).contentType("application/vnd.surok.teacher-v1+json")
                .content("{\"name\":\"Rejected\"}")
        ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/teachers/10")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.teacher-v1+json")
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("North"));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "application/pdf", "*/*;q=0", "invalid", "application/json;q=2",
            "application/json", "application/xml"
        }
    )
    void rejectsUnsupportedRepresentationBeforeFormRevision(final String accept) throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/teachers/10/edit"),
                Roles.RoleEnum.DIRECTOR
            ).header("Accept", accept)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "Rejected")
        ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/teachers/10")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.teacher-v1+json")
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("North"));
    }

    @Test
    void deniesFormMutationForViewer() throws Exception {
        final MockHttpSession session = this.session(Roles.RoleEnum.VIEWER, 1L);
        final MvcResult form = this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/teachers/create").session(session)
        ).andReturn();
        final CsrfToken token = (CsrfToken) form.getRequest().getAttribute(
            CsrfToken.class.getName()
        );
        this.mvc.perform(
            MockMvcRequestBuilders.post("/schools/1/teachers/create")
                .session(session).cookie(form.getResponse().getCookies())
                .param(token.getParameterName(), token.getToken())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "Forbidden")
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void requiresCsrfOnBrowserMutation() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.post("/schools/1/teachers/create")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "Forbidden")
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void requiresCsrfOnJsonMutation() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.post("/schools/1/teachers")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.teacher-v1+json")
                .contentType("application/vnd.surok.teacher-v1+json")
                .content("{\"name\":\"Forbidden\"}")
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void createsXmlThroughCanonicalForm() throws Exception {
        final MockHttpSession session = this.session(Roles.RoleEnum.DIRECTOR, 1L);
        final MvcResult form = this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/teachers/create").session(session)
        ).andReturn();
        final CsrfToken token = (CsrfToken) form.getRequest().getAttribute(
            CsrfToken.class.getName()
        );
        this.mvc.perform(
            MockMvcRequestBuilders.post("/schools/1/teachers/create")
                .session(session).cookie(form.getResponse().getCookies())
                .param(token.getParameterName(), token.getToken())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", " Иванов Иван Иванович ")
                .header("Accept", "application/vnd.surok.teacher-v1+xml")
        ).andExpect(MockMvcResultMatchers.status().isCreated()).andExpect(
            MockMvcResultMatchers.header().string(
                "Location", Matchers.startsWith("/schools/1/teachers/")
            )
        ).andExpect(MockMvcResultMatchers.xpath("/teacher/name").string("Иванов Иван Иванович"));
    }

    @Test
    void removesThroughCanonicalRouteWithCsrfHeader() throws Exception {
        final MockHttpSession session = this.session(Roles.RoleEnum.DIRECTOR, 1L);
        final MvcResult form = this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/teachers/10/edit").session(session)
        ).andReturn();
        final CsrfToken token = (CsrfToken) form.getRequest().getAttribute(
            CsrfToken.class.getName()
        );
        this.mvc.perform(
            MockMvcRequestBuilders.delete("/schools/1/teachers/10")
                .session(session).cookie(form.getResponse().getCookies())
                .header(token.getHeaderName(), token.getToken())
        ).andExpect(MockMvcResultMatchers.status().isNoContent());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/teachers/10").session(session)
        ).andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/", "/10", "/create", "/fragment", "/10/edit"})
    void doesNotExposeApiAliases(final String suffix) throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get(
                String.format("/api/schools/1/teachers%s", suffix)
            ).session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.teacher-v1+json")
        ).andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void rejectsDuplicateRevisionWithoutChangingTheTeacher() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.put("/schools/1/teachers/10"), Roles.RoleEnum.DIRECTOR
            ).accept("application/vnd.surok.teacher-v1+json")
                .contentType("application/vnd.surok.teacher-v1+json")
                .content("{\"name\":\"South\"}")
        ).andExpect(MockMvcResultMatchers.status().isConflict());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/teachers/10")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L))
                .accept("application/vnd.surok.teacher-v1+json")
        ).andExpect(MockMvcResultMatchers.jsonPath("$.name").value("North"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/create", "/11/edit"})
    void redirectsDuplicateForms(final String suffix) throws Exception {
        final String path = String.format("/schools/1/teachers%s", suffix);
        this.mvc.perform(
            this.authorized(MockMvcRequestBuilders.post(path), Roles.RoleEnum.DIRECTOR)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", " North ")
        ).andExpect(MockMvcResultMatchers.status().isFound()).andExpect(
            MockMvcResultMatchers.redirectedUrl(String.format("%s?error=duplicate", path))
        );
        this.mvc.perform(
            MockMvcRequestBuilders.get(path).param("error", "duplicate")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().string(
                Matchers.containsString("Учитель с таким именем уже существует")
            )
        );
    }

    private MockHttpServletRequestBuilder authorized(
        final MockHttpServletRequestBuilder request, final Roles.RoleEnum role
    ) throws Exception {
        final MockHttpSession session = this.session(role, 1L);
        final MvcResult form = this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/teachers/create").session(session)
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
        registry.add("spring.datasource.url", ThsHttpTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", ThsHttpTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", ThsHttpTest.POSTGRES::getPassword);
    }
}
