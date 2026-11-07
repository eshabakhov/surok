/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school;

import com.eshabakhov.surok.User;
import com.eshabakhov.surok.school.input.SlInput;
import com.eshabakhov.surok.school.input.SlInputName;
import com.eshabakhov.surok.user.AuthUser;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrPostgres;
import com.eshabakhov.surok.user.UrsPostgres;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
    "PMD.UnitTestShouldIncludeAssert", "PMD.UnitTestContainsTooManyAsserts"
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

    /** Verifies staff can list schools to which they belong. */
    @Test
    void restrictsSchoolCollectionsToMemberships() throws Exception {
        this.prepare();
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools").with(
                SecurityMockMvcRequestPostProcessors.user(this.principal(Roles.RoleEnum.DIRECTOR))
            )
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().string(Matchers.containsString("Школа №3"))
        ).andExpect(
            MockMvcResultMatchers.content().string(
                Matchers.not(Matchers.containsString("Школа №4"))
            )
        );
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

    /** A global subscription does not permit a viewer to revise a school. */
    @Test
    void deniesSchoolRevisionToSubscribedViewer() throws Exception {
        this.prepare();
        final AuthUser viewer = this.principal(Roles.RoleEnum.VIEWER);
        viewer.roles().grant(Roles.RoleEnum.BASIC_MAKER);
        this.mvc.perform(
            MockMvcRequestBuilders.put("/schools/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Чужое название\"}")
                .accept("application/vnd.surok.school-v1+json")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .with(SecurityMockMvcRequestPostProcessors.user(viewer))
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    /** A director may revise the school to which they belong. */
    @Test
    void revisesSchoolForItsDirector() throws Exception {
        this.prepare();
        this.mvc.perform(
            MockMvcRequestBuilders.put("/schools/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Новая школа №3\"}")
                .accept("application/vnd.surok.school-v1+json")
                .with(SecurityMockMvcRequestPostProcessors.csrf()).with(
                    SecurityMockMvcRequestPostProcessors.user(
                        this.principal(Roles.RoleEnum.DIRECTOR)
                    )
                )
        ).andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Новая школа №3"));
    }

    /** An administrator may remove a school through its protected collection. */
    @Test
    void removesSchoolForAnAdministrator() throws Exception {
        this.prepare();
        this.mvc.perform(
            MockMvcRequestBuilders.delete("/schools/4")
                .with(SecurityMockMvcRequestPostProcessors.csrf()).with(
                    SecurityMockMvcRequestPostProcessors.user(
                        this.principal(Roles.RoleEnum.ADMIN)
                    )
                )
        ).andExpect(MockMvcResultMatchers.status().isNoContent());
    }

    /** A director can add an existing account, assign a role, and remove membership. */
    @Test
    void managesExistingSchoolMembers() throws Exception {
        this.prepare();
        final AuthUser director = this.principal(Roles.RoleEnum.DIRECTOR);
        final User member = new UrsPostgres(this.ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.org", UUID.randomUUID()), false
        );
        final String path = String.format("/schools/3/users/%d", member.uid());
        this.mvc.perform(
            MockMvcRequestBuilders.put(path).with(
                SecurityMockMvcRequestPostProcessors.user(director)
            ).with(SecurityMockMvcRequestPostProcessors.csrf())
                .accept("application/vnd.surok.school-user.v1+json")
        ).andExpect(MockMvcResultMatchers.status().isCreated());
        this.mvc.perform(
            MockMvcRequestBuilders.post(String.format("%s/roles", path))
                .with(SecurityMockMvcRequestPostProcessors.user(director))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType("application/vnd.surok.school-role.v1+json")
                .accept("application/vnd.surok.school-role.v1+json")
                .content("{\"role\":\"TEACHER\"}")
        ).andExpect(MockMvcResultMatchers.status().isCreated());
        MatcherAssert.assertThat(
            new UrsPostgres(this.ctx, 3L).roles(member).list().getFirst().name(),
            Matchers.is("TEACHER")
        );
        this.mvc.perform(
            MockMvcRequestBuilders.delete(path).with(
                SecurityMockMvcRequestPostProcessors.user(director)
            ).with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(MockMvcResultMatchers.status().isNoContent());
        MatcherAssert.assertThat(
            new UrsPostgres(this.ctx, 3L).contains(member), Matchers.is(false)
        );
        MatcherAssert.assertThat(
            new UrsPostgres(this.ctx).contains(member), Matchers.is(true)
        );
    }

    /** A director can find and attach an existing account by login. */
    @Test
    void attachesExistingAccountByLogin() throws Exception {
        this.prepare();
        final String login = UUID.randomUUID().toString();
        final User member = new UrsPostgres(this.ctx).register(
            login, "ValidPassword1!",
            String.format("%s@example.org", UUID.randomUUID()), false
        );
        this.mvc.perform(
            MockMvcRequestBuilders.post("/schools/3/users").with(
                SecurityMockMvcRequestPostProcessors.user(
                    this.principal(Roles.RoleEnum.DIRECTOR)
                )
            ).with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType("application/vnd.surok.school-user.v1+json")
                .accept("application/vnd.surok.school-user.v1+json")
                .content(String.format("{\"login\":\"%s\"}", login))
        ).andExpect(MockMvcResultMatchers.status().isCreated());
        MatcherAssert.assertThat(
            new UrsPostgres(this.ctx, 3L).contains(member), Matchers.is(true)
        );
    }

    /** A member without the director role cannot add another account. */
    @Test
    void deniesMemberManagementToNonDirectors() throws Exception {
        this.prepare();
        final AuthUser viewer = this.principal(Roles.RoleEnum.VIEWER);
        final User target = new UrsPostgres(this.ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.org", UUID.randomUUID()), false
        );
        this.mvc.perform(
            MockMvcRequestBuilders.put(
                String.format("/schools/3/users/%d", target.uid())
            ).with(SecurityMockMvcRequestPostProcessors.user(viewer))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .accept("application/vnd.surok.school-user.v1+json")
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
        MatcherAssert.assertThat(
            new UrsPostgres(this.ctx, 3L).contains(target), Matchers.is(false)
        );
    }

    /** A school viewer may inspect its members through the protected collection. */
    @Test
    void allowsSchoolViewerToReadMembers() throws Exception {
        this.prepare();
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/3/users")
                .accept("application/vnd.surok.school-users.v1+json").with(
                    SecurityMockMvcRequestPostProcessors.user(
                        this.principal(Roles.RoleEnum.VIEWER)
                    )
                )
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().string(Matchers.containsString("user"))
        );
    }

    /** A global subscription does not permit granting a school role. */
    @Test
    void deniesRoleGrantToSubscribedViewer() throws Exception {
        this.prepare();
        final AuthUser viewer = this.principal(Roles.RoleEnum.VIEWER);
        viewer.roles().grant(Roles.RoleEnum.BASIC_MAKER);
        this.mvc.perform(
            MockMvcRequestBuilders.post(
                String.format("/schools/3/users/%d/roles", viewer.uid())
            ).contentType("application/vnd.surok.school-role.v1+json")
                .accept("application/vnd.surok.school-role.v1+json")
                .content("{\"role\":\"TEACHER\"}")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .with(SecurityMockMvcRequestPostProcessors.user(viewer))
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    /** A member of one school cannot inspect another school's users. */
    @Test
    void deniesReadingAnotherSchoolsMembers() throws Exception {
        this.prepare();
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/4/users")
                .accept("application/vnd.surok.school-users.v1+json").with(
                    SecurityMockMvcRequestPostProcessors.user(
                        this.principal(Roles.RoleEnum.VIEWER)
                    )
                )
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    /** A new director can manage the school without refreshing their login session. */
    @Test
    void recognizesNewDirectorWithinAnExistingSession() throws Exception {
        this.prepare();
        final User creator = new UrsPostgres(this.ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.org", UUID.randomUUID()), false
        );
        final AuthUser principal = new AuthUser(creator);
        final UsernamePasswordAuthenticationToken session =
            new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
            );
        final long school = creator.schools().create(
            new SlInputName(new SlInput(), "Новая школа")
        ).uid();
        MatcherAssert.assertThat(creator.schools().contains(school), Matchers.is(true));
        this.mvc.perform(
            MockMvcRequestBuilders.put(
                String.format(
                    "/schools/%d/users/%d", school,
                    new UrsPostgres(this.ctx).register(
                        UUID.randomUUID().toString(), "ValidPassword1!",
                        String.format("%s@example.org", UUID.randomUUID()), false
                    ).uid()
                )
            ).with(SecurityMockMvcRequestPostProcessors.authentication(session))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .accept("application/vnd.surok.school-user.v1+json")
        ).andExpect(MockMvcResultMatchers.status().isCreated());
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
                INSERT INTO public."user" (username, password, email, corporate)
                VALUES (?, 'secret', ?, true) RETURNING id
                """,
                UUID.randomUUID().toString(),
                String.format("%s@example.org", UUID.randomUUID())
            ).fetchSingle("id", Long.class)
        );
        new UrsPostgres(this.ctx, 3L).add(user);
        if (role == Roles.RoleEnum.ADMIN) {
            user.roles().grant(role);
        } else {
            new UrsPostgres(this.ctx, 3L).roles(user).grant(role);
        }
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
