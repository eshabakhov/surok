/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.building.cabinet;

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
 * Existing cabinet HTTP contracts verified with MockMvc assertions.
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
final class CbsHttpTest {

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
    CbsHttpTest(final DSLContext ctx, final MockMvc mvc) {
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
                INSERT INTO public.cabinet (id, building_id, name, is_deleted)
                VALUES (30, 10, 'Room', false), (31, 10, 'Room annex', false),
                    (32, 10, 'Room removed', true), (40, 11, 'Room other building', false),
                    (50, 20, 'Room foreign', false)
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
            MockMvcRequestBuilders.get("/schools/1/buildings/10/cabinets")
                .accept(MediaType.TEXT_HTML)
        ).andExpect(MockMvcResultMatchers.status().isFound())
            .andExpect(MockMvcResultMatchers.redirectedUrl("http://localhost/users/login"));
    }

    @Test
    void printsJsonPage() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10/cabinets")
                .accept("application/vnd.surok.cabinets.v1+json")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L))
                .param("name_ct", "room").param("limit", "1").param("offset", "2")
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.items.length()").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[0].name").value("Room annex"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.page.total").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$.page.limit").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.page.offset").value(2));
    }

    @Test
    void printsJsonCabinet() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10/cabinets/30")
                .accept("application/vnd.surok.cabinet-v1+json")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L))
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().json("{\"id\":30,\"name\":\"Room\"}"));
    }

    @Test
    void deniesAnotherSchool() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/2/buildings/20/cabinets/50")
                .accept("application/vnd.surok.cabinet-v1+json")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void deniesJsonCreationForViewer() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/buildings/10/cabinets"),
                Roles.RoleEnum.VIEWER
            ).accept("application/vnd.surok.cabinet-v1+json")
                .contentType("application/vnd.surok.cabinet-v1+json")
                .content("{\"name\":\"East\"}")
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void deniesCabinetCreationWithOnlyAGlobalSubscription() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/buildings/10/cabinets"),
                Roles.RoleEnum.BASIC_MAKER
            ).accept("application/vnd.surok.cabinet-v1+json")
                .contentType("application/vnd.surok.cabinet-v1+json")
                .content("{\"name\":\"Незаконный кабинет\"}")
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void createsCabinetForDeputyDirector() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/buildings/10/cabinets"),
                Roles.RoleEnum.DEPUTY_DIRECTOR
            ).accept("application/vnd.surok.cabinet-v1+json")
                .contentType("application/vnd.surok.cabinet-v1+json")
                .content("{\"name\":\"Кабинет заместителя\"}")
        ).andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Кабинет заместителя"));
    }

    /**
     * Verifies the collection is the only cabinet creation address.
     *
     * @param suffix Removed route suffix
     * @param type Former request format
     */
    @ParameterizedTest
    @CsvSource({
        "/, application/vnd.surok.cabinet-v1+json",
        "/create, application/vnd.surok.cabinet-v1+json",
        "/create, application/x-www-form-urlencoded"
    })
    void rejectsLegacyCabinetCreationRoutes(final String suffix, final String type)
        throws Exception {
        final MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(
            String.format("/schools/1/buildings/10/cabinets%s", suffix)
        ).contentType(type);
        if (MediaType.APPLICATION_FORM_URLENCODED_VALUE.equals(type)) {
            request.param("name", "Кабинет из старой формы");
        } else {
            request.content("{\"name\":\"Кабинет из старого API\"}");
        }
        this.mvc.perform(this.authorized(request, Roles.RoleEnum.DIRECTOR))
            .andExpect(MockMvcResultMatchers.status().is4xxClientError());
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "application/json", "application/xml",
            "application/vnd.surok.cabinet-v2+json",
            "application/vnd.surok.cabinets.v1+json"
        }
    )
    void rejectsUnsupportedInputBeforeMutation(final String type) throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/buildings/10/cabinets"),
                Roles.RoleEnum.DIRECTOR
            ).contentType(type).accept("application/vnd.surok.cabinet-v1+json")
                .content("{\"name\":\"Rejected\"}")
        ).andExpect(MockMvcResultMatchers.status().isUnsupportedMediaType());
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.put("/schools/1/buildings/10/cabinets/30"),
                Roles.RoleEnum.DIRECTOR
            ).contentType(type).accept("application/vnd.surok.cabinet-v1+json")
                .content("{\"name\":\"Rejected\"}")
        ).andExpect(MockMvcResultMatchers.status().isUnsupportedMediaType());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10/cabinets")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.cabinets.v1+json").param("name_ct", "Rejected")
        ).andExpect(MockMvcResultMatchers.jsonPath("$.page.total").value(0));
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10/cabinets/30")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.cabinet-v1+json")
        ).andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Room"));
    }

    @Test
    void createsCabinetFromJson() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/buildings/10/cabinets"),
                Roles.RoleEnum.DIRECTOR
            ).accept("application/vnd.surok.cabinet-v1+json")
                .contentType("application/vnd.surok.cabinet-v1+json")
                .content("{\"name\":\"East\"}")
        ).andExpect(MockMvcResultMatchers.status().isCreated()).andExpect(
            MockMvcResultMatchers.header().string(
                "Location", Matchers.startsWith("/schools/1/buildings/10/cabinets/")
            )
        ).andExpect(MockMvcResultMatchers.jsonPath("$.name").value("East"));
    }

    @ParameterizedTest
    @ValueSource(longs = {32L, 40L, 50L, 999L})
    void rejectsRevisionOfUnavailableCabinet(final long identifier) throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.put("/schools/1/buildings/10/cabinets/{id}", identifier),
                Roles.RoleEnum.DIRECTOR
            ).accept("application/vnd.surok.cabinet-v1+json")
                .contentType("application/vnd.surok.cabinet-v1+json")
                .content("{\"name\":\"West\"}")
        ).andExpect(MockMvcResultMatchers.status().isNotFound());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10/cabinets")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.cabinets.v1+json").param("name_ct", "West")
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.page.total").value(0));
    }

    @Test
    void revisesCabinetFromJson() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.put("/schools/1/buildings/10/cabinets/30"),
                Roles.RoleEnum.DIRECTOR
            ).accept("application/vnd.surok.cabinet-v1+json")
                .contentType("application/vnd.surok.cabinet-v1+json")
                .content("{\"name\":\"Renamed\"}")
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().json("{\"id\":30,\"name\":\"Renamed\"}")
        );
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|', value = {
            "''|Room", "/create|Новый кабинет", "/30|Room", "/30/edit|Room"
        }
    )
    void rendersCabinetPages(final String path, final String content) throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get(
                String.format("/schools/1/buildings/10/cabinets%s", path)
            ).session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().string(Matchers.containsString(content))
        );
    }

    /** Verifies a blank name is still reported on the native creation form. */
    @Test
    void redirectsInvalidCabinetCreation() throws Exception {
        final String form = "/schools/1/buildings/10/cabinets/create";
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/buildings/10/cabinets"),
                Roles.RoleEnum.DIRECTOR
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
            MockMvcRequestBuilders.get("/schools/1/buildings/10/cabinets")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.cabinets.v1+json")
        ).andExpect(MockMvcResultMatchers.jsonPath("$.page.total").value(2));
    }

    /**
     * Verifies data clients still receive validation errors when creating from form fields.
     *
     * @param accept Response representation
     */
    @ParameterizedTest
    @ValueSource(
        strings = {"application/vnd.surok.cabinet-v1+json", "application/vnd.surok.cabinet-v1+xml"}
    )
    void keepsApiValidationForCreationFormFields(final String accept) throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/buildings/10/cabinets"),
                Roles.RoleEnum.DIRECTOR
            ).header("Accept", accept)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", " ")
        ).andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.header().doesNotExist("Location"));
    }

    /** Verifies the edit page supplies a CSRF token accepted by the JSON update. */
    @Test
    void revisesThroughTheEditPagesCsrfHeader() throws Exception {
        final MockHttpSession session = this.session(Roles.RoleEnum.DIRECTOR, 1L);
        final MvcResult form = this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10/cabinets/30/edit").session(session)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        final CsrfToken token = (CsrfToken) form.getRequest().getAttribute(
            CsrfToken.class.getName()
        );
        this.mvc.perform(
            MockMvcRequestBuilders.put("/schools/1/buildings/10/cabinets/30")
                .session(session).cookie(form.getResponse().getCookies())
                .header(token.getHeaderName(), token.getToken())
                .contentType("application/vnd.surok.cabinet-v1+json")
                .accept("application/vnd.surok.cabinet-v1+json")
                .content("{\"name\":\" Кабинет 204 \"}")
        ).andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Кабинет 204"));
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10/cabinets/30").session(session)
                .accept("application/vnd.surok.cabinet-v1+json")
        ).andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Кабинет 204"));
    }

    @Test
    void createsThroughProtectedForm() throws Exception {
        final MockHttpSession session = this.session(Roles.RoleEnum.DIRECTOR, 1L);
        final MvcResult form = this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10/cabinets/create").session(session)
        ).andReturn();
        final CsrfToken token = (CsrfToken) form.getRequest().getAttribute(
            CsrfToken.class.getName()
        );
        this.mvc.perform(
            MockMvcRequestBuilders.post("/schools/1/buildings/10/cabinets")
                .session(session).cookie(form.getResponse().getCookies())
                .param(token.getParameterName(), token.getToken())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", " Web cabinet ")
        ).andExpect(MockMvcResultMatchers.status().isFound())
            .andExpect(MockMvcResultMatchers.redirectedUrl("/schools/1/buildings/10/cabinets"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"name\":\" \"}"})
    void rejectsEmptyJsonName(final String body) throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/buildings/10/cabinets"),
                Roles.RoleEnum.DIRECTOR
            ).accept("application/vnd.surok.cabinet-v1+json")
                .contentType("application/vnd.surok.cabinet-v1+json")
                .content(body)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(longs = {32L, 40L, 50L, 999L})
    void hidesDeletedAndForeignCabinets(final long cabinet) throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get(
                "/schools/1/buildings/10/cabinets/{cabinet}", cabinet
            ).accept("application/vnd.surok.cabinet-v1+json")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
        ).andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void filtersFragmentAndRetainsPagination() throws Exception {
        final String type = "text/vnd.surok.cabinets-fragment+html";
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10/cabinets")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L))
                .accept(type).param("name", "room").param("limit", "1").param("offset", "2")
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().contentTypeCompatibleWith(type)
        ).andExpect(
            MockMvcResultMatchers.header().stringValues(
                HttpHeaders.VARY, Matchers.hasItem(HttpHeaders.ACCEPT)
            )
        ).andExpect(
            MockMvcResultMatchers.content().string(Matchers.not(Matchers.containsString("<html")))
        ).andExpect(
            MockMvcResultMatchers.content().string(Matchers.containsString("Room annex"))
        ).andExpect(
            MockMvcResultMatchers.content().string(Matchers.containsString("pagination-prev"))
        ).andExpect(
            MockMvcResultMatchers.content().string(Matchers.not(Matchers.containsString("removed")))
        ).andExpect(
            MockMvcResultMatchers.content().string(Matchers.not(Matchers.containsString("foreign")))
        );
    }

    @Test
    void negotiatesJsonOnBrowserRoute() throws Exception {
        final String accept = "application/vnd.surok.cabinet-v1+json";
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10/cabinets/30")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L)).header("Accept", accept)
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(accept))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Room"));
    }

    @Test
    void negotiatesXmlOnBrowserRoute() throws Exception {
        final String accept = "application/vnd.surok.cabinet-v1+xml";
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10/cabinets/30")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L)).header("Accept", accept)
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().contentTypeCompatibleWith(accept))
            .andExpect(MockMvcResultMatchers.xpath("/cabinet/name").string("Room"));
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|', value = {
            "text/html;q=0.2,application/*;q=0.9|application/vnd.surok.cabinet-v1+json",
            "text/html;q=0,*/*;q=1|application/vnd.surok.cabinet-v1+json",
            "application/xml;q=0.1,*/*;q=0.8|text/html",
            "application/vnd.surok.cabinet-v1+xml|application/vnd.surok.cabinet-v1+xml",
            "text/html;charset=UTF-8|text/html",
            "*/*|text/html"
        }
    )
    void honorsAcceptPriorities(final String accept, final String expected) throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10/cabinets/30")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L)).header("Accept", accept)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().contentTypeCompatibleWith(expected)
        );
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "application/pdf", "*/*;q=0", "invalid", "application/json;q=2",
            "application/com.eshabakhov.surok.school.cabinet.simplecabinet+json",
            "application/vnd.surok.cabinets.v1+json"
        }
    )
    void rejectsUnavailableRepresentations(final String accept) throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10/cabinets/30")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L)).header("Accept", accept)
        ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/json", "application/xml"})
    void rejectsUnversionedRepresentations(final String accept) throws Exception {
        for (final String suffix : List.of("", "/create", "/30", "/30/edit")) {
            this.mvc.perform(
                MockMvcRequestBuilders.get(
                    String.format("/schools/1/buildings/10/cabinets%s", suffix)
                ).session(this.session(Roles.RoleEnum.VIEWER, 1L)).header("Accept", accept)
            ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
        }
    }

    @ParameterizedTest
    @CsvSource({"ASC, Room, Room annex", "DESC, Room annex, Room"})
    void sortsByName(final String direction, final String first, final String second)
        throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10/cabinets")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L))
                .accept("application/vnd.surok.cabinets.v1+json")
                .param("sort", String.format("name:%s", direction))
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[0].name").value(first))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[1].name").value(second));
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10/cabinets")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L))
                .accept("text/vnd.surok.cabinets-fragment+html")
                .param("sort", String.format("name:%s", direction))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().string(
                Matchers.containsString("class=\"cabinets-sort active\"")
            )
        );
    }

    @Test
    void printsVersionedCollection() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10/cabinets")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L))
                .header("Accept", "application/vnd.surok.cabinets.v1+json")
                .param("filter", "name:Room").param("limit", "1")
                .param("sort", "name:DESC")
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.items.length()").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.page.limit").value(1));
    }

    /**
     * Verifies removed list aliases no longer return the cabinet collection.
     *
     * @param suffix Removed route suffix
     */
    @ParameterizedTest
    @ValueSource(strings = {"/", "/fragment"})
    void rejectsLegacyCabinetListRoutes(final String suffix) throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get(String.format("/schools/1/buildings/10/cabinets%s", suffix))
                .session(this.session(Roles.RoleEnum.VIEWER, 1L)).accept(MediaType.TEXT_HTML)
        ).andExpect(MockMvcResultMatchers.status().is4xxClientError());
    }

    /**
     * Verifies the creation page renders HTML only when the client accepts it.
     *
     * @param accept Unsupported response representation
     */
    @ParameterizedTest
    @ValueSource(strings = {"application/vnd.surok.cabinets.v1+json", "*/*;q=0"})
    void rejectsUnacceptableRepresentationsAtTheCabinetCreationForm(final String accept)
        throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10/cabinets/create")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept(accept)
        ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
    }

    /** Verifies a fragment cannot expose cabinets from a building outside the school. */
    @Test
    void scopesCabinetFragmentsToTheRequestedSchool() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/20/cabinets")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("text/vnd.surok.cabinets-fragment+html")
        ).andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    /** Verifies a blank edit is reported to the browser as a validation error. */
    @Test
    void rejectsBlankCabinetNamesOnUpdate() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.put("/schools/1/buildings/10/cabinets/30"),
                Roles.RoleEnum.DIRECTOR
            ).accept("application/vnd.surok.cabinet-v1+json")
                .contentType("application/vnd.surok.cabinet-v1+json")
                .content("{\"name\":\" \"}")
        ).andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    void rendersXsltWithNavigationAndToken() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10/cabinets/create")
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
                MockMvcRequestBuilders.post("/schools/1/buildings/10/cabinets"),
                Roles.RoleEnum.DIRECTOR
            ).header("Accept", accept).contentType("application/vnd.surok.cabinet-v1+json")
                .content("{\"name\":\"Rejected\"}")
        ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10/cabinets")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.cabinets.v1+json").param("name_ct", "Rejected")
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
                MockMvcRequestBuilders.post("/schools/1/buildings/10/cabinets"),
                Roles.RoleEnum.DIRECTOR
            ).header("Accept", accept)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "Rejected")
        ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10/cabinets")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.cabinets.v1+json").param("name_ct", "Rejected")
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
                MockMvcRequestBuilders.put("/schools/1/buildings/10/cabinets/30"),
                Roles.RoleEnum.DIRECTOR
            ).header("Accept", accept).contentType("application/vnd.surok.cabinet-v1+json")
                .content("{\"name\":\"Rejected\"}")
        ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10/cabinets/30")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.cabinet-v1+json")
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Room"));
    }

    /** Verifies the edit page URL cannot process a legacy form update. */
    @Test
    void rejectsTheLegacyCabinetUpdateRoute() throws Exception {
        this.mvc.perform(
            this.authorized(
                MockMvcRequestBuilders.post("/schools/1/buildings/10/cabinets/30/edit"),
                Roles.RoleEnum.DIRECTOR
            ).accept(MediaType.TEXT_HTML)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "Отклонённое название")
        ).andExpect(MockMvcResultMatchers.status().isMethodNotAllowed());
    }

    @Test
    void deniesFormMutationForViewer() throws Exception {
        final MockHttpSession session = this.session(Roles.RoleEnum.VIEWER, 1L);
        final MvcResult form = this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10/cabinets/create").session(session)
        ).andReturn();
        final CsrfToken token = (CsrfToken) form.getRequest().getAttribute(
            CsrfToken.class.getName()
        );
        this.mvc.perform(
            MockMvcRequestBuilders.post("/schools/1/buildings/10/cabinets")
                .session(session).cookie(form.getResponse().getCookies())
                .param(token.getParameterName(), token.getToken())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "Forbidden")
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void requiresCsrfOnBrowserMutation() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.post("/schools/1/buildings/10/cabinets")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", "Forbidden")
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void requiresCsrfOnJsonMutation() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.post("/schools/1/buildings/10/cabinets")
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.cabinet-v1+json")
                .contentType("application/vnd.surok.cabinet-v1+json")
                .content("{\"name\":\"Forbidden\"}")
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    void createsXmlThroughCanonicalForm() throws Exception {
        final MockHttpSession session = this.session(Roles.RoleEnum.DIRECTOR, 1L);
        final MvcResult form = this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10/cabinets/create").session(session)
        ).andReturn();
        final CsrfToken token = (CsrfToken) form.getRequest().getAttribute(
            CsrfToken.class.getName()
        );
        this.mvc.perform(
            MockMvcRequestBuilders.post("/schools/1/buildings/10/cabinets")
                .session(session).cookie(form.getResponse().getCookies())
                .param(token.getParameterName(), token.getToken())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("name", " Актовый зал ")
                .header("Accept", "application/vnd.surok.cabinet-v1+xml")
        ).andExpect(MockMvcResultMatchers.status().isCreated()).andExpect(
            MockMvcResultMatchers.header().string(
                "Location", Matchers.startsWith("/schools/1/buildings/10/cabinets/")
            )
        ).andExpect(MockMvcResultMatchers.xpath("/cabinet/name").string("Актовый зал"));
    }

    @Test
    void removesThroughCanonicalRouteWithCsrfHeader() throws Exception {
        final MockHttpSession session = this.session(Roles.RoleEnum.DIRECTOR, 1L);
        final MvcResult form = this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10/cabinets/30/edit").session(session)
        ).andReturn();
        final CsrfToken token = (CsrfToken) form.getRequest().getAttribute(
            CsrfToken.class.getName()
        );
        this.mvc.perform(
            MockMvcRequestBuilders.delete("/schools/1/buildings/10/cabinets/30")
                .session(session).cookie(form.getResponse().getCookies())
                .header(token.getHeaderName(), token.getToken())
        ).andExpect(MockMvcResultMatchers.status().isNoContent());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10/cabinets/30").session(session)
        ).andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/", "/30", "/create", "/fragment", "/30/edit"})
    void doesNotExposeApiAliases(final String suffix) throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get(
                String.format("/api/schools/1/buildings/10/cabinets%s", suffix)
            ).session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
                .accept("application/vnd.surok.cabinet-v1+json")
        ).andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    private MockHttpServletRequestBuilder authorized(
        final MockHttpServletRequestBuilder request, final Roles.RoleEnum role
    ) throws Exception {
        final MockHttpSession session = this.session(role, 1L);
        final MvcResult form = this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings/10/cabinets/create").session(session)
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
        registry.add("spring.datasource.url", CbsHttpTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", CbsHttpTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", CbsHttpTest.POSTGRES::getPassword);
    }
}
