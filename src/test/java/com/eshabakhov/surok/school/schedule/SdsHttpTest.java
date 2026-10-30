/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule;

import com.eshabakhov.surok.User;
import com.eshabakhov.surok.user.AuthUser;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrPostgres;
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
 * Existing schedule HTTP contracts verified with MockMvc assertions.
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
final class SdsHttpTest {

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
    SdsHttpTest(final DSLContext ctx, final MockMvc mvc) {
        this.ctx = ctx;
        this.mvc = mvc;
    }

    @BeforeEach
    void insertSchoolsAndSchedules() {
        this.ctx.resultQuery(
            """
                INSERT INTO public.school (id, name)
                VALUES (1, 'First school'), (2, 'Second school')
                """
        ).execute();
        this.ctx.resultQuery(
            """
                INSERT INTO public.schedule (id, school_id, name, is_deleted)
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
            MockMvcRequestBuilders.get("/schools/1/schedules").accept(MediaType.TEXT_HTML)
        ).andExpect(MockMvcResultMatchers.status().isFound())
            .andExpect(MockMvcResultMatchers.redirectedUrl("http://localhost/users/login"));
    }

    @Test
    void printsJsonPage() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules/")
                .accept("application/vnd.surok.schedules.v1+json")
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
    void printsJsonSchedule() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules/10")
                .accept("application/vnd.surok.schedule-v1+json")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L))
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().json("{\"id\":10,\"name\":\"North\"}"));
    }

    @Test
    void deniesAnotherSchool() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/2/schedules/20")
                .accept("application/vnd.surok.schedule-v1+json")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void deniesJsonCreationForViewer() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/schedules"), Roles.RoleEnum.VIEWER
            ).accept("application/vnd.surok.schedule-v1+json")
                .contentType("application/vnd.surok.schedule-v1+json")
                .content("{\"name\":\"East\"}")
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/", "/create"})
    void createsThroughJsonAliases(final String suffix) throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post(
                    String.format("%s%s", "/schools/1/schedules", suffix)
                ), Roles.RoleEnum.DIRECTOR
            ).contentType("application/vnd.surok.schedule-v1+json")
                .accept("application/vnd.surok.schedule-v1+json").content("{\"name\":\"East\"}")
        ).andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("East"));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "application/json", "application/xml",
            "application/vnd.surok.schedule-v2+json",
            "application/vnd.surok.schedules.v1+json"
        }
    )
    void rejectsUnsupportedInputBeforeMutation(final String type) throws Exception {
        for (final String suffix : List.of("", "/", "/create", "/10/edit")) {
            this.mvc.perform(
                this.authorized(
                    MockMvcRequestBuilders.post(
                        String.format("%s%s", "/schools/1/schedules", suffix)
                    ), Roles.RoleEnum.DIRECTOR
                ).contentType(type).accept("application/vnd.surok.schedule-v1+json")
                    .content("{\"name\":\"Rejected\"}")
            ).andExpect(MockMvcResultMatchers.status().isUnsupportedMediaType());
        }
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.put("/schools/1/schedules/10"),
                Roles.RoleEnum.DIRECTOR
            ).contentType(type).accept("application/vnd.surok.schedule-v1+json")
                .content("{\"name\":\"Rejected\"}")
        ).andExpect(MockMvcResultMatchers.status().isUnsupportedMediaType());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.schedules.v1+json").param("name_ct", "Rejected")
        ).andExpect(MockMvcResultMatchers.jsonPath("$.page.total").value(0));
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules/10")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.schedule-v1+json")
        ).andExpect(MockMvcResultMatchers.jsonPath("$.name").value("North"));
    }

    @Test
    void createsScheduleFromJson() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/schedules"),
                Roles.RoleEnum.DIRECTOR
            ).accept("application/vnd.surok.schedule-v1+json")
                .contentType("application/vnd.surok.schedule-v1+json")
                .content("{\"name\":\"East\"}")
        ).andExpect(MockMvcResultMatchers.status().isCreated()).andExpect(
            MockMvcResultMatchers.header().string(
                "Location", Matchers.startsWith("/schools/1/schedules/")
            )
        ).andExpect(MockMvcResultMatchers.jsonPath("$.name").value("East"));
    }

    @ParameterizedTest
    @ValueSource(longs = {12L, 20L, 999L})
    void rejectsRevisionOfUnavailableSchedule(final long identifier) throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.put("/schools/1/schedules/{id}", identifier),
                Roles.RoleEnum.DIRECTOR
            ).accept("application/vnd.surok.schedule-v1+json")
                .contentType("application/vnd.surok.schedule-v1+json")
                .content("{\"name\":\"West\"}")
        ).andExpect(MockMvcResultMatchers.status().isNotFound());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.schedules.v1+json").param("name_ct", "West")
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.page.total").value(0));
    }

    @Test
    void revisesScheduleFromJson() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.put("/schools/1/schedules/10"), Roles.RoleEnum.DIRECTOR
            ).accept("application/vnd.surok.schedule-v1+json")
                .contentType("application/vnd.surok.schedule-v1+json")
                .content("{\"name\":\"Renamed\"}")
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().json("{\"id\":10,\"name\":\"Renamed\"}")
        );
    }

    @Test
    void rendersSchedulePages() throws Exception {
        for (final String path : List.of(
            "", "/fragment", "/create", "/10", "/10/edit"
        )) {
            this.mvc.perform(
                MockMvcRequestBuilders.get(String.format("/schools/1/schedules%s", path))
                    .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
            ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
                MockMvcResultMatchers.content().string(Matchers.not(Matchers.emptyString()))
            );
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/create", "/10/edit"})
    void redirectsInvalidBrowserForms(final String suffix) throws Exception {
        final String form = String.format("/schools/1/schedules%s", suffix);
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
                Matchers.containsString("Название не может быть пустым")
            )
        );
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.schedules.v1+json")
        ).andExpect(MockMvcResultMatchers.jsonPath("$.page.total").value(2));
    }

    @ParameterizedTest
    @CsvSource({
        "/create, application/vnd.surok.schedule-v1+json",
        "/create, application/vnd.surok.schedule-v1+xml",
        "/10/edit, application/vnd.surok.schedule-v1+json",
        "/10/edit, application/vnd.surok.schedule-v1+xml"
    })
    void keepsApiValidationForFormFields(final String suffix, final String accept)
        throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post(String.format("/schools/1/schedules%s", suffix)),
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
                MockMvcRequestBuilders.post("/schools/1/schedules/10/edit"),
                Roles.RoleEnum.DIRECTOR
            ).accept(MediaType.TEXT_HTML)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", " Revised through form ")
        ).andExpect(MockMvcResultMatchers.status().isFound())
            .andExpect(MockMvcResultMatchers.redirectedUrl("/schools/1/schedules/10"));
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules/10")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.schedule-v1+json")
        ).andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Revised through form"));
    }

    @Test
    void createsThroughProtectedForm() throws Exception {
        final MockHttpSession session = this.session(Roles.RoleEnum.DIRECTOR, 1L);
        final MvcResult form = this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules/create").session(session)
        ).andReturn();
        final CsrfToken token = (CsrfToken) form.getRequest().getAttribute(
            CsrfToken.class.getName()
        );
        this.mvc.perform(
            MockMvcRequestBuilders.post("/schools/1/schedules/create")
                .session(session).cookie(form.getResponse().getCookies())
                .param(token.getParameterName(), token.getToken())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", " Web schedule ")
        ).andExpect(MockMvcResultMatchers.status().isFound())
            .andExpect(MockMvcResultMatchers.redirectedUrl("/schools/1/schedules"));
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
            MockMvcRequestBuilders.get(String.format("/schools/1/schedules%s", suffix))
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
        final String accept = "application/vnd.surok.schedule-v1+json";
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules/10")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L)).header("Accept", accept)
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(accept))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("North"));
    }

    @Test
    void negotiatesXmlOnBrowserRoute() throws Exception {
        final String accept = "application/vnd.surok.schedule-v1+xml";
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules/10")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L)).header("Accept", accept)
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(accept))
            .andExpect(MockMvcResultMatchers.xpath("/schedule/name").string("North"));
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|', value = {
            "text/html;q=0.2,application/*;q=0.9|application/vnd.surok.schedule-v1+json",
            "text/html;q=0,*/*;q=1|application/vnd.surok.schedule-v1+json",
            "application/xml;q=0.1,*/*;q=0.8|text/html",
            "application/vnd.surok.schedule-v1+xml|application/vnd.surok.schedule-v1+xml",
            "text/html;charset=UTF-8|text/html",
            "*/*|text/html"
        }
    )
    void honorsAcceptPriorities(final String accept, final String expected) throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules/10")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L)).header("Accept", accept)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().contentTypeCompatibleWith(expected)
        );
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "application/pdf", "*/*;q=0", "invalid", "application/json;q=2",
            "application/com.eshabakhov.surok.school.schedule.simpleschedule+json",
            "application/vnd.surok.schedules.v1+json"
        }
    )
    void rejectsUnavailableRepresentations(final String accept) throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules/10")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L)).header("Accept", accept)
        ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/json", "application/xml"})
    void rejectsUnversionedRepresentations(final String accept) throws Exception {
        for (final String suffix : List.of("", "/", "/fragment", "/create", "/10", "/10/edit")) {
            this.mvc.perform(
                MockMvcRequestBuilders.get(String.format("%s%s", "/schools/1/schedules", suffix))
                    .session(this.session(Roles.RoleEnum.VIEWER, 1L)).header("Accept", accept)
            ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
        }
    }

    @ParameterizedTest
    @CsvSource({"ASC, North, South", "DESC, South, North"})
    void sortsByName(final String direction, final String first, final String second)
        throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L))
                .accept("application/vnd.surok.schedules.v1+json")
                .param("sort", String.format("name:%s", direction))
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[0].name").value(first))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[1].name").value(second));
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules/fragment")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L)).accept(MediaType.TEXT_HTML)
                .param("sort", String.format("name:%s", direction))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().string(
                Matchers.containsString("class=\"schedules-sort active\"")
            )
        );
    }

    @Test
    void printsVersionedCollection() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L))
                .header("Accept", "application/vnd.surok.schedules.v1+json")
                .param("filter", "name:North").param("limit", "1")
                .param("sort", "name:DESC")
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.items.length()").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.page.limit").value(1));
    }

    @Test
    void rendersXsltWithNavigationAndToken() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules/create")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .header("Accept", "text/html,application/xhtml+xml;q=0.9,*/*;q=0.8")
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().contentTypeCompatibleWith("text/html")
        ).andExpect(
            MockMvcResultMatchers.content().string(Matchers.containsString("name=\"_csrf\""))
        ).andExpect(
            MockMvcResultMatchers.content().string(
                Matchers.containsString("href=\"/schools/1/schedules\"")
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
                MockMvcRequestBuilders.post("/schools/1/schedules"),
                Roles.RoleEnum.DIRECTOR
            ).header("Accept", accept).contentType("application/vnd.surok.schedule-v1+json")
                .content("{\"name\":\"Rejected\"}")
        ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.schedules.v1+json").param("name_ct", "Rejected")
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
                MockMvcRequestBuilders.post("/schools/1/schedules/create"),
                Roles.RoleEnum.DIRECTOR
            ).header("Accept", accept)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "Rejected")
        ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.schedules.v1+json").param("name_ct", "Rejected")
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
                MockMvcRequestBuilders.put("/schools/1/schedules/10"),
                Roles.RoleEnum.DIRECTOR
            ).header("Accept", accept).contentType("application/vnd.surok.schedule-v1+json")
                .content("{\"name\":\"Rejected\"}")
        ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules/10")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.schedule-v1+json")
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
                MockMvcRequestBuilders.post("/schools/1/schedules/10/edit"),
                Roles.RoleEnum.DIRECTOR
            ).header("Accept", accept)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "Rejected")
        ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules/10")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.schedule-v1+json")
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("North"));
    }

    @Test
    void deniesFormMutationForViewer() throws Exception {
        final MockHttpSession session = this.session(Roles.RoleEnum.VIEWER, 1L);
        final MvcResult form = this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules/create").session(session)
        ).andReturn();
        final CsrfToken token = (CsrfToken) form.getRequest().getAttribute(
            CsrfToken.class.getName()
        );
        this.mvc.perform(
            MockMvcRequestBuilders.post("/schools/1/schedules/create")
                .session(session).cookie(form.getResponse().getCookies())
                .param(token.getParameterName(), token.getToken())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "Forbidden")
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void requiresCsrfOnBrowserMutation() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.post("/schools/1/schedules/create")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "Forbidden")
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void requiresCsrfOnJsonMutation() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.post("/schools/1/schedules")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.schedule-v1+json")
                .contentType("application/vnd.surok.schedule-v1+json")
                .content("{\"name\":\"Forbidden\"}")
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void createsXmlThroughCanonicalForm() throws Exception {
        final MockHttpSession session = this.session(Roles.RoleEnum.DIRECTOR, 1L);
        final MvcResult form = this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules/create").session(session)
        ).andReturn();
        final CsrfToken token = (CsrfToken) form.getRequest().getAttribute(
            CsrfToken.class.getName()
        );
        this.mvc.perform(
            MockMvcRequestBuilders.post("/schools/1/schedules/create")
                .session(session).cookie(form.getResponse().getCookies())
                .param(token.getParameterName(), token.getToken())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", " Расписание 2026/2027 ")
                .header("Accept", "application/vnd.surok.schedule-v1+xml")
        ).andExpect(MockMvcResultMatchers.status().isCreated()).andExpect(
            MockMvcResultMatchers.header().string(
                "Location", Matchers.startsWith("/schools/1/schedules/")
            )
        ).andExpect(MockMvcResultMatchers.xpath("/schedule/name").string("Расписание 2026/2027"));
    }

    @Test
    void removesThroughCanonicalRouteWithCsrfHeader() throws Exception {
        final MockHttpSession session = this.session(Roles.RoleEnum.DIRECTOR, 1L);
        final MvcResult form = this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules/10/edit").session(session)
        ).andReturn();
        final CsrfToken token = (CsrfToken) form.getRequest().getAttribute(
            CsrfToken.class.getName()
        );
        this.mvc.perform(
            MockMvcRequestBuilders.delete("/schools/1/schedules/10")
                .session(session).cookie(form.getResponse().getCookies())
                .header(token.getHeaderName(), token.getToken())
        ).andExpect(MockMvcResultMatchers.status().isNoContent());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules/10").session(session)
        ).andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/", "/10", "/create", "/fragment", "/10/edit"})
    void doesNotExposeApiAliases(final String suffix) throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get(
                String.format("/api/schools/1/schedules%s", suffix)
            ).session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.schedule-v1+json")
        ).andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void rejectsDuplicateRevisionWithoutChangingTheSchedule() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.put("/schools/1/schedules/10"), Roles.RoleEnum.DIRECTOR
            ).accept("application/vnd.surok.schedule-v1+json")
                .contentType("application/vnd.surok.schedule-v1+json")
                .content("{\"name\":\"South\"}")
        ).andExpect(MockMvcResultMatchers.status().isConflict());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules/10")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L))
                .accept("application/vnd.surok.schedule-v1+json")
        ).andExpect(MockMvcResultMatchers.jsonPath("$.name").value("North"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/create", "/11/edit"})
    void redirectsDuplicateForms(final String suffix) throws Exception {
        final String path = String.format("/schools/1/schedules%s", suffix);
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
                Matchers.containsString("Расписание с таким названием уже существует")
            )
        );
    }

    @Test
    void preservesSummaryAndLoadsWhenRenamingSchedule() throws Exception {
        this.ctx.resultQuery(
            """
            INSERT INTO public.school_class (id, school_id, grade, litera, is_deleted)
            VALUES (30, 1, 5, 'А', false), (31, 1, 6, 'Б', false),
                (32, 1, 7, 'В', true), (40, 2, 8, 'Г', false)
            """
        ).execute();
        this.ctx.resultQuery(
            """
            INSERT INTO subject (id, school_id, origin, name)
        VALUES (50, 1, 'SCHOOL', 'Математика'),
        (60, 2, 'SCHOOL', 'Другой предмет')
            """
        ).execute();
        this.ctx.resultQuery(
            """
            INSERT INTO public.class_curriculum
                (id, schedule_id, school_class_id, subject_id, hours)
            VALUES (100, 10, 30, 50, 5), (101, 10, 31, 50, 3), (102, 20, 40, 60, 2)
            """
        ).execute();
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.put("/schools/1/schedules/10"), Roles.RoleEnum.DIRECTOR
            ).accept("application/vnd.surok.schedule-v1+json")
                .contentType("application/vnd.surok.schedule-v1+json")
                .content("{\"name\":\"Renamed\"}")
        ).andExpect(MockMvcResultMatchers.status().isOk());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules/10")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L)).accept(MediaType.TEXT_HTML)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().string(
                Matchers.allOf(
                    Matchers.containsString("<span>Renamed</span>"),
                    Matchers.containsString("class=\"stat-value\">2</div>"),
                    Matchers.containsString("class=\"stat-value\">1</div>"),
                    Matchers.containsString("href=\"/schools/1/schedules/10/loads\"")
                )
            )
        );
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules/10/loads")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L))
                .accept("application/vnd.surok.class-loads.v1+json")
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.page.total").value(2)).andExpect(
                MockMvcResultMatchers.jsonPath("$.items[0].subject.name").value("Математика")
            );
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules/10/loads/draft")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L)).accept(MediaType.TEXT_HTML)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().string(Matchers.containsString("Renamed"))
        );
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.delete("/schools/1/schedules/10"), Roles.RoleEnum.DIRECTOR
            )
        ).andExpect(MockMvcResultMatchers.status().isNoContent());
        for (final String suffix : List.of("", "/loads", "/loads/100", "/loads/draft")) {
            this.mvc.perform(
                MockMvcRequestBuilders.get(String.format("/schools/1/schedules/10%s", suffix))
                    .session(this.session(Roles.RoleEnum.VIEWER, 1L)).accept(MediaType.TEXT_HTML)
            ).andExpect(MockMvcResultMatchers.status().isNotFound());
        }
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L)).accept(MediaType.TEXT_HTML)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().string(
                Matchers.allOf(
                    Matchers.not(Matchers.containsString("Renamed")),
                    Matchers.not(Matchers.containsString("North removed")),
                    Matchers.not(Matchers.containsString("North foreign"))
                )
            )
        );
    }

    private MockHttpServletRequestBuilder authorized(
        final MockHttpServletRequestBuilder request, final Roles.RoleEnum role
    ) throws Exception {
        final MockHttpSession session = this.session(role, 1L);
        final MvcResult form = this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules/create").session(session)
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
                INSERT INTO public."user" (username, password, email, corporate, school_id)
                VALUES (?, 'secret', ?, true, ?) RETURNING id
                """,
                UUID.randomUUID().toString(),
                String.format("%s@example.org", UUID.randomUUID()),
                school
            ).fetchSingle("id", Long.class)
        );
        user.roles().grant(role);
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
        registry.add("spring.datasource.url", SdsHttpTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", SdsHttpTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", SdsHttpTest.POSTGRES::getPassword);
    }
}
