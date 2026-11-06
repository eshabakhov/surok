/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load;

import com.eshabakhov.surok.User;
import com.eshabakhov.surok.user.AuthUser;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrPostgres;
import com.eshabakhov.surok.user.UrsPostgres;
import java.util.List;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Unified teaching load HTTP contracts verified with MockMvc assertions.
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
        "TRUNCATE TABLE public.school, subject RESTART IDENTITY CASCADE",
        """
        INSERT INTO public.school (id, name) VALUES (1, 'Школа'), (2, 'Чужая школа');
        """,
        """
        INSERT INTO public.schedule (id, school_id, name, is_deleted)
        VALUES (100, 1, 'Год', false), (101, 1, 'Полугодие', false),
            (102, 1, 'Удалённое расписание', true), (200, 2, 'Чужой год', false);
        """,
        """
        INSERT INTO public.school_class (id, school_id, grade, litera, is_deleted)
        VALUES (10, 1, 2, 'А', false), (11, 1, 10, 'Б', false),
            (12, 1, 3, 'Удалён', true), (20, 2, 7, 'Чужой', false);
        """,
        """
        INSERT INTO subject (id, school_id, origin, name, is_deleted)
        VALUES (30, 1, 'SCHOOL', 'Математика', false),
        (31, 1, 'SCHOOL', 'Физика', false),
        (32, 1, 'SCHOOL', 'Удалённый предмет', true),
        (40, 2, 'SCHOOL', 'Чужой предмет', false);
        """,
        """
        INSERT INTO public.class_curriculum
            (id, schedule_id, school_class_id, subject_id, hours)
        VALUES (50, 100, 10, 30, 5), (51, 100, 11, 31, 3),
            (52, 101, 10, 30, 2), (60, 200, 20, 40, 1);
        """
    }
)
@SuppressWarnings({
    "PMD.UnusedPrivateMethod", "UnusedMethod",
    "PMD.UnitTestShouldIncludeAssert"
})
final class ClsHttpTest {

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
    ClsHttpTest(final DSLContext ctx, final MockMvc mvc) {
        this.ctx = ctx;
        this.mvc = mvc;
    }

    @Test
    void requiresAuthentication() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules/100/loads")
        ).andExpect(MockMvcResultMatchers.status().isFound());
    }

    @Test
    void printsOnlyRequestedPageAndSchedule() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules/100/loads/")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L))
                .accept("application/vnd.surok.class-loads.v1+json")
                .param("limit", "1").param("offset", "2")
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.items.length()").value(1))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[0].id").value(51))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[0].schoolClass.name").value("10Б"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.page.total").value(2))
            .andExpect(MockMvcResultMatchers.jsonPath("$.page.offset").value(2));
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules/100/loads")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L))
                .accept("application/vnd.surok.class-loads.v1+xml")
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.xpath("count(/loads/load)").number(2.0));
    }

    @Test
    void negotiatesEntityFormats() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules/100/loads/50")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L))
                .accept("application/vnd.surok.class-load-v1+json")
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.schoolClass.id").value(10))
            .andExpect(MockMvcResultMatchers.jsonPath("$.subject.name").value("Математика"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.hours").value(5));
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/schedules/100/loads/50")
                .session(this.session(Roles.RoleEnum.VIEWER, 1L))
                .accept("application/vnd.surok.class-load-v1+xml")
        ).andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.xpath("/load/schoolClass/name").string("2А"));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "", "/", "/fragment", "/50"
        }
    )
    void rendersXsltPages(final String suffix) throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get(String.format("/schools/1/schedules/100/loads%s", suffix))
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().string(Matchers.not(Matchers.emptyString()))
        );
    }

    @ParameterizedTest
    @ValueSource(longs = {102L, 200L, 999L})
    void scopesSchedules(final long schedule) throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get(String.format("/schools/1/schedules/%d/loads", schedule))
                .session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
        ).andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "", "/50", "/create"
        }
    )
    void exposesNoApiAliases(final String suffix) throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get(
                String.format("/api/schools/1/schedules/100/loads%s", suffix)
            ).session(this.session(Roles.RoleEnum.DIRECTOR, 1L))
        ).andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/create", "/50/edit"})
    void exposesNoManualForms(final String suffix) throws Exception {
        MatcherAssert.assertThat(
            "A removed manual load form was still accessible",
            this.mvc.perform(
                MockMvcRequestBuilders.get(
                    String.format("/schools/1/schedules/100/loads%s", suffix)
                ).session(this.session(Roles.RoleEnum.ADMIN, 1L))
            ).andReturn().getResponse().getStatus(),
            Matchers.is(404)
        );
    }

    @ParameterizedTest
    @CsvSource({"POST, ''", "POST, /", "POST, /create", "PUT, /50", "DELETE, /50"})
    void refusesDirectChangesWithValidCredentials(final String method, final String suffix)
        throws Exception {
        MatcherAssert.assertThat(
            "A direct API request changed the persisted loads outside the draft",
            List.of(
                this.mvc.perform(
                    MockMvcRequestBuilders.request(
                        HttpMethod.valueOf(method),
                        String.format("/schools/1/schedules/100/loads%s", suffix)
                    ).session(this.session(Roles.RoleEnum.ADMIN, 1L))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType("application/vnd.surok.class-load-v1+json")
                        .accept("application/vnd.surok.class-load-v1+json")
                        .content("{\"schoolClassId\":10,\"subjectId\":31,\"hours\":9}")
                ).andReturn().getResponse().getStatus(),
                this.ctx.fetch("SELECT id, hours FROM class_curriculum ORDER BY id")
                    .map(row -> List.of(row.get(0), row.get(1)))
            ),
            Matchers.contains(
                Matchers.oneOf(404, 405),
                Matchers.equalTo(
                    List.of(List.of(50L, 5), List.of(51L, 3), List.of(52L, 2), List.of(60L, 1))
                )
            )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"/create", "/50/edit"})
    void refusesManualFormSubmissions(final String suffix) throws Exception {
        MatcherAssert.assertThat(
            "A removed form submission changed the applied hours",
            List.of(
                this.mvc.perform(
                    MockMvcRequestBuilders.post(
                        String.format("/schools/1/schedules/100/loads%s", suffix)
                    ).session(this.session(Roles.RoleEnum.ADMIN, 1L))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("schoolClassId", "10").param("subjectId", "31")
                        .param("hours", "9")
                ).andReturn().getResponse().getStatus(),
                this.ctx.fetch("SELECT hours FROM class_curriculum ORDER BY id")
                    .getValues(0, Integer.class)
            ),
            Matchers.contains(Matchers.oneOf(404, 405), Matchers.equalTo(List.of(5, 3, 2, 1)))
        );
    }

    @ParameterizedTest
    @ValueSource(longs = {52L, 60L, 999L})
    void scopesLoadsToTheRequestedSchedule(final long identifier) throws Exception {
        MatcherAssert.assertThat(
            "The load endpoint exposed another schedule's load",
            this.mvc.perform(
                MockMvcRequestBuilders.get(
                    String.format("/schools/1/schedules/100/loads/%d", identifier)
                ).session(this.session(Roles.RoleEnum.VIEWER, 1L))
            ).andReturn().getResponse().getStatus(),
            Matchers.is(404)
        );
    }

    @ParameterizedTest
    @CsvSource({"DIRECTOR, 403", "VIEWER, 403", "ADMIN, 200"})
    void restrictsAnotherSchoolsLoads(final Roles.RoleEnum role, final int status)
        throws Exception {
        MatcherAssert.assertThat(
            "The load endpoint ignored school access permissions",
            this.mvc.perform(
                MockMvcRequestBuilders.get("/schools/2/schedules/200/loads")
                    .session(this.session(role, 1L))
            ).andReturn().getResponse().getStatus(),
            Matchers.is(status)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/json", "application/xml", "application/pdf", "*/*;q=0"})
    void rejectsUnsupportedRepresentations(final String accept) throws Exception {
        MatcherAssert.assertThat(
            "The applied loads endpoint returned an unsupported representation",
            this.mvc.perform(
                MockMvcRequestBuilders.get("/schools/1/schedules/100/loads")
                    .session(this.session(Roles.RoleEnum.VIEWER, 1L)).header("Accept", accept)
            ).andReturn().getResponse().getStatus(),
            Matchers.is(406)
        );
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
        registry.add("spring.datasource.url", ClsHttpTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", ClsHttpTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", ClsHttpTest.POSTGRES::getPassword);
    }
}
