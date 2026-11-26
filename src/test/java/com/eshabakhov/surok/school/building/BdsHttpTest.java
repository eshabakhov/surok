/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.building;

import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.security.OaYaml;
import com.eshabakhov.surok.security.Operations;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrAcAuth;
import com.eshabakhov.surok.user.UrAcPostgres;
import com.eshabakhov.surok.user.UrsPostgres;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
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
 * Existing building HTTP contracts verified with MockMvc assertions.
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
final class BdsHttpTest {

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
    BdsHttpTest(final DSLContext ctx, final MockMvc mvc) {
        this.ctx = ctx;
        this.mvc = mvc;
    }

    @BeforeEach
    void insertSchoolsAndBuildings() {
        this.ctx.resultQuery(
            """
                INSERT INTO public.school (id, name)
                VALUES (1, 'First school'), (2, 'Second school')
                """
        ).execute();
        this.ctx.resultQuery(
            """
                INSERT INTO public.building (id, school_id, name, is_deleted)
                VALUES (10, 1, 'North', false), (11, 1, 'South', false),
                    (12, 1, 'North removed', true), (20, 2, 'North foreign', false)
                """
        ).execute();
        this.ctx.resultQuery(
            """
                INSERT INTO public.cabinet (id, building_id, name)
                VALUES (30, 10, 'Room')
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
            MockMvcRequestBuilders.get("/schools/1/buildings").accept(MediaType.TEXT_HTML)
        ).andExpect(MockMvcResultMatchers.status().isFound())
            .andExpect(MockMvcResultMatchers.redirectedUrl("http://localhost/users/login"));
    }

    @Test
    void printsJsonPage() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings")
                .accept("application/vnd.surok.buildings.v1+json")
                .session(this.session(Roles.RoleEnum.STUDENT, 1L))
                .param("name_ct", "north").param("limit", "1")
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.items.length()").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[0].name").value("North"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.page.total").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.page.limit").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.page.offset").value(1));
    }

    @Test
    void printsJsonBuilding() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10")
                .accept("application/vnd.surok.building-v1+json")
                .session(this.session(Roles.RoleEnum.STUDENT, 1L))
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().json("{\"id\":10,\"name\":\"North\"}"));
    }

    @Test
    void deniesAnotherSchool() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/2/buildings/20")
                .accept("application/vnd.surok.building-v1+json")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void deniesJsonCreationForStudent() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/buildings"), Roles.RoleEnum.STUDENT
            ).accept("application/vnd.surok.building-v1+json")
                .contentType("application/vnd.surok.building-v1+json")
                .content("{\"name\":\"East\"}")
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void deniesBuildingCreationWithOnlyAGlobalSubscription() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/buildings"),
                Roles.RoleEnum.BASIC_MAKER
            ).accept("application/vnd.surok.building-v1+json")
                .contentType("application/vnd.surok.building-v1+json")
                .content("{\"name\":\"Незаконный корпус\"}")
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    /**
     * Allows school editors and an administrator without membership to create a resource.
     *
     * @param role Acting role
     * @throws Exception If creation fails
     */
    @ParameterizedTest
    @CsvSource({"ADMIN", "DIRECTOR", "DEPUTY_DIRECTOR"})
    void createsBuildingForAnEditingRole(final Roles.RoleEnum role) throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/buildings"), role
            ).accept("application/vnd.surok.building-v1+json")
                .contentType("application/vnd.surok.building-v1+json")
                .content("{\"name\":\"Корпус для занятий\"}")
        ).andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Корпус для занятий"));
    }

    /**
     * Verifies the collection is the only building creation address.
     *
     * @param suffix Removed route suffix
     * @param type Former request format
     */
    @ParameterizedTest
    @CsvSource({
        "/, application/vnd.surok.building-v1+json",
        "/create, application/vnd.surok.building-v1+json",
        "/create, application/x-www-form-urlencoded"
    })
    void rejectsLegacyBuildingCreationRoutes(final String suffix, final String type)
        throws Exception {
        final MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(
            String.format("/schools/1/buildings%s", suffix)
        ).contentType(type);
        if (MediaType.APPLICATION_FORM_URLENCODED_VALUE.equals(type)) {
            request.param("name", "Корпус из старой формы");
        } else {
            request.content("{\"name\":\"Корпус из старого API\"}");
        }
        this.mvc.perform(this.authorized(request, Roles.RoleEnum.DIRECTOR))
            .andExpect(MockMvcResultMatchers.status().is4xxClientError());
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "application/json", "application/xml",
            "application/vnd.surok.building-v2+json",
            "application/vnd.surok.buildings.v1+json"
        }
    )
    void rejectsUnsupportedInputBeforeMutation(final String type) throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/buildings"), Roles.RoleEnum.DIRECTOR
            ).contentType(type).accept("application/vnd.surok.building-v1+json")
                .content("{\"name\":\"Rejected\"}")
        ).andExpect(MockMvcResultMatchers.status().isUnsupportedMediaType());
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.put("/schools/1/buildings/10"),
                Roles.RoleEnum.DIRECTOR
            ).contentType(type).accept("application/vnd.surok.building-v1+json")
                .content("{\"name\":\"Rejected\"}")
        ).andExpect(MockMvcResultMatchers.status().isUnsupportedMediaType());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.buildings.v1+json").param("name_ct", "Rejected")
        ).andExpect(MockMvcResultMatchers.jsonPath("$.page.total").value(0));
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.building-v1+json")
        ).andExpect(MockMvcResultMatchers.jsonPath("$.name").value("North"));
    }

    @Test
    void createsBuildingFromJson() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/buildings"),
                Roles.RoleEnum.DIRECTOR
            ).accept("application/vnd.surok.building-v1+json")
                .contentType("application/vnd.surok.building-v1+json")
                .content("{\"name\":\"East\"}")
        ).andExpect(MockMvcResultMatchers.status().isCreated()).andExpect(
            MockMvcResultMatchers.header().string(
                "Location", Matchers.startsWith("/schools/1/buildings/")
            )
        ).andExpect(MockMvcResultMatchers.jsonPath("$.name").value("East"));
    }

    @ParameterizedTest
    @ValueSource(longs = {12L, 20L, 999L})
    void rejectsRevisionOfUnavailableBuilding(final long identifier) throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.put("/schools/1/buildings/{id}", identifier),
                Roles.RoleEnum.DIRECTOR
            ).accept("application/vnd.surok.building-v1+json")
                .contentType("application/vnd.surok.building-v1+json")
                .content("{\"name\":\"West\"}")
        ).andExpect(MockMvcResultMatchers.status().isNotFound());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.buildings.v1+json").param("name_ct", "West")
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.page.total").value(0));
    }

    @Test
    void revisesBuildingFromJson() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.put("/schools/1/buildings/10"), Roles.RoleEnum.DIRECTOR
            ).accept("application/vnd.surok.building-v1+json")
                .contentType("application/vnd.surok.building-v1+json")
                .content("{\"name\":\"Renamed\"}")
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().json("{\"id\":10,\"name\":\"Renamed\"}")
        );
    }

    @Test
    void rendersBuildingAndCabinetPages() throws Exception {
        for (final String path : List.of(
            "", "/create", "/10", "/10/edit",
            "/10/cabinets", "/10/cabinets/create", "/10/cabinets/30",
            "/10/cabinets/30/edit"
        )) {
            this.mvc.perform(
                MockMvcRequestBuilders.get(String.format("/schools/1/buildings%s", path))
                    .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
            ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
                MockMvcResultMatchers.content().string(Matchers.not(Matchers.emptyString()))
            );
        }
    }

    /** Verifies a blank name is still reported on the native creation form. */
    @Test
    void redirectsInvalidBuildingCreation() throws Exception {
        final String form = "/schools/1/buildings/create";
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/buildings"), Roles.RoleEnum.DIRECTOR
            ).accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
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
            MockMvcRequestBuilders.get("/schools/1/buildings")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.buildings.v1+json")
        ).andExpect(MockMvcResultMatchers.jsonPath("$.page.total").value(2));
    }

    /**
     * Verifies data clients still receive validation errors when creating from form fields.
     *
     * @param accept Response representation
     */
    @ParameterizedTest
    @ValueSource(
        strings = {
            "application/vnd.surok.building-v1+json", "application/vnd.surok.building-v1+xml"
        }
    )
    void keepsApiValidationForCreationFormFields(final String accept) throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/buildings"),
                Roles.RoleEnum.DIRECTOR
            ).header("Accept", accept)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", " ")
        ).andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.header().doesNotExist("Location"));
    }

    /** Verifies the protected edit preserves access to the building's cabinets. */
    @Test
    void revisesThroughTheEditPagesCsrfHeader() throws Exception {
        final MockHttpSession session = this.session(Roles.RoleEnum.DIRECTOR, 1L);
        final MvcResult form = this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10/edit").session(session)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        final CsrfToken token = (CsrfToken) form.getRequest().getAttribute(
            CsrfToken.class.getName()
        );
        this.mvc.perform(
            MockMvcRequestBuilders.put("/schools/1/buildings/10")
                .session(session).cookie(form.getResponse().getCookies())
                .header(token.getHeaderName(), token.getToken())
                .contentType("application/vnd.surok.building-v1+json")
                .accept("application/vnd.surok.building-v1+json")
                .content("{\"name\":\" Северный корпус \"}")
        ).andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Северный корпус"));
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10/cabinets/30").session(session)
                .accept("application/vnd.surok.cabinet-v1+json")
        ).andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Room"));
    }

    @Test
    void createsThroughProtectedForm() throws Exception {
        final MockHttpSession session = this.session(Roles.RoleEnum.DIRECTOR, 1L);
        final MvcResult form = this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/create").session(session)
        ).andReturn();
        final CsrfToken token = (CsrfToken) form.getRequest().getAttribute(
            CsrfToken.class.getName()
        );
        this.mvc.perform(
            MockMvcRequestBuilders.post("/schools/1/buildings")
                .session(session).cookie(form.getResponse().getCookies())
                .param(token.getParameterName(), token.getToken())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", " Web building ")
        ).andExpect(MockMvcResultMatchers.status().isFound())
            .andExpect(MockMvcResultMatchers.redirectedUrl("/schools/1/buildings"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/10/cabinets"})
    void keepsSchoolSchedulesInNavigation(final String suffix) throws Exception {
        this.ctx.resultQuery(
            """
                INSERT INTO public.schedule (id, school_id, name, is_deleted)
                VALUES (100, 1, 'Учебный год', false), (200, 2, 'Чужое расписание', false)
                """
        ).execute();
        this.mvc.perform(
            MockMvcRequestBuilders.get(String.format("/schools/1/buildings%s", suffix))
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
        final String accept = "application/vnd.surok.building-v1+json";
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10")
                .session(this.session(Roles.RoleEnum.STUDENT, 1L)).header("Accept", accept)
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(accept))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("North"));
    }

    @Test
    void negotiatesXmlOnBrowserRoute() throws Exception {
        final String accept = "application/vnd.surok.building-v1+xml";
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10")
                .session(this.session(Roles.RoleEnum.STUDENT, 1L)).header("Accept", accept)
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(accept))
            .andExpect(MockMvcResultMatchers.xpath("/building/name").string("North"));
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|', value = {
            "text/html;q=0.2,application/*;q=0.9|application/vnd.surok.building-v1+json",
            "text/html;q=0,*/*;q=1|application/vnd.surok.building-v1+json",
            "application/xml;q=0.1,*/*;q=0.8|text/html",
            "application/vnd.surok.building-v1+xml|application/vnd.surok.building-v1+xml",
            "text/html;charset=UTF-8|text/html",
            "*/*|text/html"
        }
    )
    void honorsAcceptPriorities(final String accept, final String expected) throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10")
                .session(this.session(Roles.RoleEnum.STUDENT, 1L)).header("Accept", accept)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().contentTypeCompatibleWith(expected)
        );
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "application/pdf", "*/*;q=0", "invalid", "application/json;q=2",
            "application/com.eshabakhov.surok.school.building.simplebuilding+json",
            "application/vnd.surok.buildings.v1+json"
        }
    )
    void rejectsUnavailableRepresentations(final String accept) throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10")
                .session(this.session(Roles.RoleEnum.STUDENT, 1L)).header("Accept", accept)
        ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/json", "application/xml"})
    void rejectsUnversionedRepresentations(final String accept) throws Exception {
        for (final String suffix : List.of("", "/create", "/10", "/10/edit")) {
            this.mvc.perform(
                MockMvcRequestBuilders.get(String.format("%s%s", "/schools/1/buildings", suffix))
                    .session(this.session(Roles.RoleEnum.STUDENT, 1L)).header("Accept", accept)
            ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
        }
    }

    @ParameterizedTest
    @CsvSource({"ASC, North, South", "DESC, South, North"})
    void sortsByName(final String direction, final String first, final String second)
        throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings")
                .session(this.session(Roles.RoleEnum.STUDENT, 1L))
                .accept("application/vnd.surok.buildings.v1+json")
                .param("sort", String.format("name:%s", direction))
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[0].name").value(first))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[1].name").value(second));
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings")
                .session(this.session(Roles.RoleEnum.STUDENT, 1L))
                .accept("text/vnd.surok.buildings-fragment+html")
                .param("sort", String.format("name:%s", direction))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().string(
                Matchers.containsString("class=\"buildings-sort active\"")
            )
        );
    }

    @Test
    void printsVersionedCollection() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings")
                .session(this.session(Roles.RoleEnum.STUDENT, 1L))
                .header("Accept", "application/vnd.surok.buildings.v1+json")
                .param("filter", "name:North").param("limit", "1")
                .param("sort", "name:DESC")
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.items.length()").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.page.limit").value(1));
    }

    /** Verifies the negotiated fragment keeps filtering and omits the page chrome. */
    @Test
    void rendersFilteredBuildingResultsAsAFragment() throws Exception {
        final String type = "text/vnd.surok.buildings-fragment+html";
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings")
                .session(this.session(Roles.RoleEnum.STUDENT, 1L)).accept(type)
                .param("filter", "name:North").param("limit", "1")
        ).andExpectAll(
            MockMvcResultMatchers.status().isOk(),
            MockMvcResultMatchers.content().contentTypeCompatibleWith(type),
            MockMvcResultMatchers.header().stringValues(
                HttpHeaders.VARY, Matchers.hasItem(HttpHeaders.ACCEPT)
            ),
            MockMvcResultMatchers.content().string(
                Matchers.allOf(
                    Matchers.containsString("id=\"buildings-results\""),
                    Matchers.containsString("North"),
                    Matchers.not(Matchers.containsString("South")),
                    Matchers.not(Matchers.containsString("<html"))
                )
            )
        );
    }

    /**
     * Verifies removed list aliases no longer return the building collection.
     *
     * @param suffix Removed route suffix
     */
    @ParameterizedTest
    @ValueSource(strings = {"/", "/fragment"})
    void rejectsLegacyBuildingListRoutes(final String suffix) throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get(String.format("/schools/1/buildings%s", suffix))
                .session(this.session(Roles.RoleEnum.STUDENT, 1L)).accept(MediaType.TEXT_HTML)
        ).andExpect(MockMvcResultMatchers.status().is4xxClientError());
    }

    /**
     * Verifies the creation page only renders HTML when the client accepts it.
     *
     * @param accept Unsupported response representation
     */
    @ParameterizedTest
    @ValueSource(strings = {"application/vnd.surok.buildings.v1+json", "*/*;q=0"})
    void rejectsUnacceptableRepresentationsAtTheBuildingCreationForm(final String accept)
        throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/create")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept(accept)
        ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
    }

    /** Verifies a blank edit is reported to the browser as a validation error. */
    @Test
    void rejectsBlankBuildingNamesOnUpdate() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.put("/schools/1/buildings/10"), Roles.RoleEnum.DIRECTOR
            ).accept("application/vnd.surok.building-v1+json")
                .contentType("application/vnd.surok.building-v1+json")
                .content("{\"name\":\" \"}")
        ).andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    void rendersXsltWithNavigationAndToken() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/create")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .header("Accept", "text/html,application/xhtml+xml;q=0.9,*/*;q=0.8")
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().contentTypeCompatibleWith("text/html")
        ).andExpect(
            MockMvcResultMatchers.content().string(Matchers.containsString("name=\"_csrf\""))
        ).andExpect(
            MockMvcResultMatchers.content().string(
                Matchers.containsString("href=\"/schools/1/subjects\"")
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
                MockMvcRequestBuilders.post("/schools/1/buildings"),
                Roles.RoleEnum.DIRECTOR
            ).header("Accept", accept).contentType("application/vnd.surok.building-v1+json")
                .content("{\"name\":\"Rejected\"}")
        ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.buildings.v1+json").param("name_ct", "Rejected")
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
                MockMvcRequestBuilders.post("/schools/1/buildings"),
                Roles.RoleEnum.DIRECTOR
            ).header("Accept", accept)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "Rejected")
        ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.buildings.v1+json").param("name_ct", "Rejected")
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
                MockMvcRequestBuilders.put("/schools/1/buildings/10"),
                Roles.RoleEnum.DIRECTOR
            ).header("Accept", accept).contentType("application/vnd.surok.building-v1+json")
                .content("{\"name\":\"Rejected\"}")
        ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.building-v1+json")
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("North"));
    }

    /** Verifies the edit page URL cannot process a legacy form update. */
    @Test
    void rejectsTheLegacyBuildingUpdateRoute() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/buildings/10/edit"),
                Roles.RoleEnum.DIRECTOR
            ).accept(MediaType.TEXT_HTML)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "Отклонённое название")
        ).andExpect(MockMvcResultMatchers.status().isMethodNotAllowed());
    }

    @Test
    void deniesFormMutationForStudent() throws Exception {
        final MockHttpSession session = this.session(Roles.RoleEnum.STUDENT, 1L);
        final MvcResult form = this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/create").session(session)
        ).andReturn();
        final CsrfToken token = (CsrfToken) form.getRequest().getAttribute(
            CsrfToken.class.getName()
        );
        this.mvc.perform(
            MockMvcRequestBuilders.post("/schools/1/buildings")
                .session(session).cookie(form.getResponse().getCookies())
                .param(token.getParameterName(), token.getToken())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "Forbidden")
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void requiresCsrfOnBrowserMutation() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.post("/schools/1/buildings")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "Forbidden")
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void requiresCsrfOnJsonMutation() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.post("/schools/1/buildings")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.building-v1+json")
                .contentType("application/vnd.surok.building-v1+json")
                .content("{\"name\":\"Forbidden\"}")
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void createsXmlThroughCanonicalForm() throws Exception {
        final MockHttpSession session = this.session(Roles.RoleEnum.DIRECTOR, 1L);
        final MvcResult form = this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/create").session(session)
        ).andReturn();
        final CsrfToken token = (CsrfToken) form.getRequest().getAttribute(
            CsrfToken.class.getName()
        );
        this.mvc.perform(
            MockMvcRequestBuilders.post("/schools/1/buildings")
                .session(session).cookie(form.getResponse().getCookies())
                .param(token.getParameterName(), token.getToken())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", " Актовый зал ")
                .header("Accept", "application/vnd.surok.building-v1+xml")
        ).andExpect(MockMvcResultMatchers.status().isCreated()).andExpect(
            MockMvcResultMatchers.header().string(
                "Location", Matchers.startsWith("/schools/1/buildings/")
            )
        ).andExpect(MockMvcResultMatchers.xpath("/building/name").string("Актовый зал"));
    }

    @Test
    void removesThroughCanonicalRouteWithCsrfHeader() throws Exception {
        final MockHttpSession session = this.session(Roles.RoleEnum.DIRECTOR, 1L);
        final MvcResult form = this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10/edit").session(session)
        ).andReturn();
        final CsrfToken token = (CsrfToken) form.getRequest().getAttribute(
            CsrfToken.class.getName()
        );
        this.mvc.perform(
            MockMvcRequestBuilders.delete("/schools/1/buildings/10")
                .session(session).cookie(form.getResponse().getCookies())
                .header(token.getHeaderName(), token.getToken())
        ).andExpect(MockMvcResultMatchers.status().isNoContent());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10").session(session)
        ).andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/", "/10", "/create", "/fragment", "/10/edit"})
    void doesNotExposeApiAliases(final String suffix) throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get(
                String.format("/api/schools/1/buildings%s", suffix)
            ).session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.building-v1+json")
        ).andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    /**
     * Leaves stored buildings untouched when a student requests a mutation.
     *
     * @param method Request method
     * @param suffix Resource suffix
     * @throws Exception If the request fails
     */
    @ParameterizedTest
    @CsvSource({"POST, ''", "PUT, /10", "DELETE, /10"})
    void rejectsMutationsBeforeChangingData(final String method, final String suffix)
        throws Exception {
        final List<String> names = this.ctx.resultQuery(
            "SELECT name FROM public.building WHERE NOT is_deleted ORDER BY id"
        ).fetch("name", String.class);
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.request(
                    HttpMethod.valueOf(method),
                    String.format("/schools/1/buildings%s", suffix)
                ), Roles.RoleEnum.STUDENT
            ).contentType("application/vnd.surok.building-v1+json")
                .accept("application/vnd.surok.building-v1+json")
                .content("{\"name\":\"Запрещённое изменение\"}")
        ).andReturn();
        MatcherAssert.assertThat(
            "A forbidden building request changed stored data",
            this.ctx.resultQuery(
                "SELECT name FROM public.building WHERE NOT is_deleted ORDER BY id"
            ).fetch("name", String.class), Matchers.is(names)
        );
    }

    /**
     * Evaluates current roles and membership in the requested school on every request.
     *
     * @param change Access change after authentication
     * @param route Requested resource
     * @throws Exception If setup or the request fails
     */
    @ParameterizedTest
    @CsvSource({
        "role, /schools/1/buildings/10",
        "membership, /schools/1/buildings/10",
        "school, /schools/2/buildings/20"
    })
    void rejectsARequestWithoutTheCurrentSchoolRole(final String change, final String route)
        throws Exception {
        final MockHttpServletRequestBuilder request = this.authorized(
            MockMvcRequestBuilders.put(route), Roles.RoleEnum.DIRECTOR
        ).contentType("application/vnd.surok.building-v1+json")
            .accept("application/vnd.surok.building-v1+json")
            .content("{\"name\":\"Недоступное изменение\"}");
        final UrAccount actor = new UrAcPostgres(
            this.ctx, this.ctx.resultQuery(
                "SELECT user_id FROM public.school_user WHERE school_id = ?", 1L
            ).fetchSingle("user_id", Long.class)
        );
        final UrsPostgres members = new UrsPostgres(this.ctx, 1L);
        switch (change) {
            case "role" -> members.roles(actor).revoke(
                members.roles(actor).list().getFirst().uid()
            );
            case "membership" -> members.remove(actor.uid());
            case "school" -> {
                final UrsPostgres students = new UrsPostgres(this.ctx, 2L);
                students.add(actor);
                students.roles(actor).grant(Roles.RoleEnum.STUDENT);
            }
            case null, default -> throw new IllegalArgumentException(change);
        }
        this.mvc.perform(request).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    private MockHttpServletRequestBuilder authorized(
        final MockHttpServletRequestBuilder request, final Roles.RoleEnum role
    ) throws Exception {
        final MockHttpSession session = this.session(role, 1L);
        final MvcResult form = this.mvc.perform(
            MockMvcRequestBuilders.get("/users/profile").session(session)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        final CsrfToken token = (CsrfToken) form.getRequest().getAttribute(
            CsrfToken.class.getName()
        );
        return request.session(session).cookie(form.getResponse().getCookies())
            .header(token.getHeaderName(), token.getToken());
    }

    private MockHttpSession session(final Roles.RoleEnum role, final Long school)
        throws Exception {
        final UrAccount user = new UrAcPostgres(
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
        if (school != null && role != Roles.RoleEnum.ADMIN) {
            new UrsPostgres(this.ctx, school).add(user);
        }
        if (List.of(
            Roles.RoleEnum.DIRECTOR, Roles.RoleEnum.DEPUTY_DIRECTOR,
            Roles.RoleEnum.TEACHER, Roles.RoleEnum.STUDENT
        ).contains(role)) {
            new UrsPostgres(this.ctx, school).roles(user).grant(role);
        } else {
            user.roles().grant(role);
        }
        final UrAcAuth principal = new UrAcAuth(user);
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
        registry.add("spring.datasource.url", BdsHttpTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", BdsHttpTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", BdsHttpTest.POSTGRES::getPassword);
    }

    /**
     * Verifies one authorization before rendering a resource.
     *
     * @since 0.0.1
     */
    @Nested
    final class Authorization {

        /**
         * Authorizes a resource once, including its navigation and response printing.
         *
         * @param suffix Resource view
         * @param format Response format
         * @throws Exception If the request fails
         */
        @ParameterizedTest
        @CsvSource({"'', json", "'', xml", "/edit, json", "/edit, xml"})
        void authorizesOnlyOnce(final String suffix, final String format) throws Exception {
            final UrAccount actor = new UrsPostgres(BdsHttpTest.this.ctx).register(
                UUID.randomUUID().toString(), "ValidPassword1!",
                String.format("%s@example.org", UUID.randomUUID()), false
            );
            final UrsPostgres members = new UrsPostgres(BdsHttpTest.this.ctx, 1L);
            members.add(actor);
            members.roles(actor).grant(Roles.RoleEnum.DIRECTOR);
            final Operations catalog = new OaYaml().operations();
            final List<String> checks = new ArrayList<>(1);
            final MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", String.format("/schools/1/buildings/10%s", suffix)
            );
            request.addHeader(
                HttpHeaders.ACCEPT, String.format("application/vnd.surok.building-v1+%s", format)
            );
            new BdsHttp(
                BdsHttpTest.this.ctx, () -> {
                    checks.add("permission");
                    return catalog.iterate();
                }
            ).building(1L, 10L, request, actor);
            MatcherAssert.assertThat(
                "The controller repeated authorization while rendering the building",
                checks.size(), Matchers.is(1)
            );
        }
    }
}
