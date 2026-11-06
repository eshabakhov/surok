/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import com.eshabakhov.surok.User;
import com.eshabakhov.surok.school.SlsPostgres;
import com.eshabakhov.surok.school.input.SlInput;
import com.eshabakhov.surok.school.input.SlInputName;
import com.eshabakhov.surok.user.AuthUser;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrsPostgres;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import java.util.List;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
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
 * Verifies security chains and application version metadata in web responses.
 *
 * @since 0.0.1
 */
@Testcontainers
@DirtiesContext
@SpringBootTest(properties = "app.security.docs.enabled=true")
@AutoConfigureMockMvc
final class SecurityConfigTest {

    /** Isolated database for authentication and login attempts. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16");

    /** Maven build information. */
    @Autowired
    private BuildProperties build;

    @ParameterizedTest
    @ValueSource(
        strings = {
            "/swagger-ui/index.html", "/api-docs/swagger-config", "/api-docs", "/api-docs.yaml"
        }
    )
    void opensDocumentationWithoutAuthentication(final String path, @Autowired final MockMvc mvc)
        throws Exception {
        MatcherAssert.assertThat(
            "Documentation still required a login",
            mvc.perform(MockMvcRequestBuilders.get(path)).andReturn().getResponse().getStatus(),
            Matchers.is(HttpStatus.OK.value())
        );
    }

    @Test
    void exposesOnlyPublicOperationsBeforeAuthorization(@Autowired final MockMvc mvc)
        throws Exception {
        final JsonNode spec = new ObjectMapper().readTree(
            mvc.perform(MockMvcRequestBuilders.get("/api-docs"))
                .andReturn().getResponse().getContentAsString()
        );
        MatcherAssert.assertThat(
            "Anonymous documentation exposed protected operations or required Basic for registration",
            List.of(
                spec.path("paths").has("/users/register"),
                spec.path("paths").has("/federal/curriculums"),
                spec.path("paths").path("/users/register").path("post").path("security").isEmpty()
            ),
            Matchers.contains(true, false, true)
        );
    }

    @Test
    void keepsAdministratorDocumentationOutOfAnonymousResponses(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        final ObjectMapper mapper = new ObjectMapper();
        MatcherAssert.assertThat(
            "Documentation reused another user's operations",
            List.of(
                mapper.readTree(
                    mvc.perform(
                        MockMvcRequestBuilders.get("/api-docs").with(
                            SecurityMockMvcRequestPostProcessors.httpBasic(
                                SecurityConfigTest.administrator(ctx).credentials().username(),
                                "ValidPassword1!"
                            )
                        )
                    ).andReturn().getResponse().getContentAsString()
                ).path("paths").path("/federal/curriculums/create").has("post"),
                mapper.readTree(
                    mvc.perform(MockMvcRequestBuilders.get("/api-docs"))
                        .andReturn().getResponse().getContentAsString()
                ).path("paths").has("/federal/curriculums")
            ),
            Matchers.contains(true, false)
        );
    }

    @Test
    void filtersDocumentationForAnOrdinaryReader(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        final String name = UUID.randomUUID().toString();
        new UrsPostgres(ctx).register(
            name, "ReaderPassword1!", String.format("%s@example.org", name), false
        );
        final JsonNode paths = new ObjectMapper().readTree(
            mvc.perform(
                MockMvcRequestBuilders.get("/api-docs").with(
                    SecurityMockMvcRequestPostProcessors.httpBasic(name, "ReaderPassword1!")
                )
            ).andReturn().getResponse().getContentAsString()
        ).path("paths");
        MatcherAssert.assertThat(
            "Reader documentation omitted the catalogue or exposed administrative operations",
            List.of(
                paths.path("/federal/curriculums").has("get"),
                paths.path("/federal/curriculums/create").has("post"),
                paths.path("/federal/subjects").has("post"), paths.has("/schools")
            ),
            Matchers.contains(true, false, false, true)
        );
    }

    @ParameterizedTest
    @EnumSource(value = Roles.RoleEnum.class, names = {"DIRECTOR", "VIEWER"})
    void distinguishesSchoolAndFederalPermissions(
        final Roles.RoleEnum role, @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        final User user = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "SchoolPassword1!",
            String.format("%s@example.org", UUID.randomUUID()), true
        );
        ctx.execute("DELETE FROM user_role WHERE user_id = ?", user.uid());
        final long school = new SlsPostgres(ctx).create(
            new SlInputName(new SlInput(), "Школа документации")
        ).uid();
        new UrsPostgres(ctx, school).add(user);
        new UrsPostgres(ctx, school).roles(user).grant(role);
        final JsonNode paths = new ObjectMapper().readTree(
            mvc.perform(
                MockMvcRequestBuilders.get("/api-docs").with(
                    SecurityMockMvcRequestPostProcessors.httpBasic(
                        user.credentials().username(), "SchoolPassword1!"
                    )
                )
            ).andReturn().getResponse().getContentAsString()
        ).path("paths");
        MatcherAssert.assertThat(
            "Swagger confused permissions for federal and school routes of the same controller",
            List.of(
                paths.path("/schools/{school}/subjects").has("get"),
                paths.path("/schools/{school}/subjects").has("post"),
                paths.path("/federal/subjects").has("post"),
                paths.path("/schools/{school}/schedules/{schedule}/loads/draft").has("put")
            ),
            Matchers.contains(
                true, role == Roles.RoleEnum.DIRECTOR, false, role == Roles.RoleEnum.DIRECTOR
            )
        );
    }

    @Test
    void ignoresWebsiteSessionsInDocumentation(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        final AuthUser user = new AuthUser(SecurityConfigTest.administrator(ctx));
        final MockHttpSession session = new MockHttpSession();
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            new SecurityContextImpl(
                UsernamePasswordAuthenticationToken.authenticated(user, "", user.getAuthorities())
            )
        );
        MatcherAssert.assertThat(
            "Swagger inherited the website administrator session",
            new ObjectMapper().readTree(
                mvc.perform(MockMvcRequestBuilders.get("/api-docs").session(session))
                    .andReturn().getResponse().getContentAsString()
            ).path("paths").has("/federal/curriculums"),
            Matchers.is(false)
        );
    }

    @Test
    void rejectsIncorrectDocumentationCredentials(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        MatcherAssert.assertThat(
            "Authorize accepted an invalid password",
            mvc.perform(
                MockMvcRequestBuilders.get("/api-docs").with(
                    SecurityMockMvcRequestPostProcessors.httpBasic(
                        SecurityConfigTest.administrator(ctx).credentials().username(),
                        "Incorrect1!"
                    )
                ).header("X-Requested-With", "XMLHttpRequest")
            ).andReturn().getResponse().getStatus(),
            Matchers.is(HttpStatus.UNAUTHORIZED.value())
        );
    }

    @Test
    void avoidsBrowserPasswordDialogsOnSwaggerFailures(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        MatcherAssert.assertThat(
            "Swagger triggered the browser's native password dialog",
            mvc.perform(
                MockMvcRequestBuilders.get("/api-docs")
                    .header("X-Requested-With", "XMLHttpRequest").with(
                        SecurityMockMvcRequestPostProcessors.httpBasic(
                            SecurityConfigTest.administrator(ctx).credentials().username(),
                            "Incorrect1!"
                        )
                    )
            ).andReturn().getResponse().containsHeader(HttpHeaders.WWW_AUTHENTICATE),
            Matchers.is(false)
        );
    }

    @Test
    void preventsCachingPersonalizedDocumentation(@Autowired final MockMvc mvc) throws Exception {
        MatcherAssert.assertThat(
            "Personalized documentation allowed browser or proxy caching",
            mvc.perform(MockMvcRequestBuilders.get("/api-docs"))
                .andReturn().getResponse().getHeader(HttpHeaders.CACHE_CONTROL),
            Matchers.containsString("no-store")
        );
    }

    @Test
    void installsSwaggerAuthorizationPlugin(@Autowired final MockMvc mvc) throws Exception {
        MatcherAssert.assertThat(
            "Swagger initializer omitted credential validation",
            mvc.perform(MockMvcRequestBuilders.get("/swagger-ui/swagger-initializer.js"))
                .andReturn().getResponse().getContentAsString(),
            Matchers.containsString("window.surokSwagger.plugin")
        );
    }

    @Test
    void acceptsTheDocumentationCsrfTokenForMutations(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        final MockHttpServletResponse document = mvc.perform(
            MockMvcRequestBuilders.get("/api-docs")
        ).andReturn().getResponse();
        final String name = String.format("Астробиология-%s", UUID.randomUUID());
        MatcherAssert.assertThat(
            "Try it out could not create a subject with the supplied CSRF token",
            new ObjectMapper().readTree(
                mvc.perform(
                    MockMvcRequestBuilders.post("/federal/subjects")
                        .header("X-Surok-Swagger", "true")
                        .header("X-XSRF-TOKEN", document.getHeader("X-XSRF-TOKEN"))
                        .cookie(document.getCookie("XSRF-TOKEN"))
                        .contentType("application/vnd.surok.subject-v1+json")
                        .accept("application/vnd.surok.subject-v1+json")
                        .content(String.format("{\"name\":\"%s\"}", name)).with(
                            SecurityMockMvcRequestPostProcessors.httpBasic(
                                SecurityConfigTest.administrator(ctx).credentials().username(),
                                "ValidPassword1!"
                            )
                        )
                ).andReturn().getResponse().getContentAsString()
            ).path("name").asText(),
            Matchers.is(name)
        );
    }

    @Test
    void keepsCsrfProtectionForSwaggerMutations(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        MatcherAssert.assertThat(
            "Swagger bypassed CSRF protection",
            mvc.perform(
                MockMvcRequestBuilders.post("/federal/subjects")
                    .header("X-Surok-Swagger", "true")
                    .contentType("application/vnd.surok.subject-v1+json")
                    .accept("application/vnd.surok.subject-v1+json")
                    .content("{\"name\":\"Астрономия\"}").with(
                        SecurityMockMvcRequestPostProcessors.httpBasic(
                            SecurityConfigTest.administrator(ctx).credentials().username(),
                            "ValidPassword1!"
                        )
                    )
            ).andReturn().getResponse().getStatus(),
            Matchers.is(HttpStatus.FORBIDDEN.value())
        );
    }

    /**
     * A valid password must reach the existing success handler.
     *
     * @param mvc HTTP client
     * @param ctx Database context
     * @throws Exception If login fails
     */
    @Test
    void acceptsFormLogin(@Autowired final MockMvc mvc, @Autowired final DSLContext ctx)
        throws Exception {
        Assertions.assertEquals(
            "/schools",
            mvc.perform(
                SecurityMockMvcRequestBuilders.formLogin()
                    .user(SecurityConfigTest.administrator(ctx).credentials().username())
                    .password("ValidPassword1!")
            ).andReturn().getResponse().getRedirectedUrl(),
            "Valid credentials did not reach the login success handler"
        );
    }

    /**
     * A wrong password must reach the existing failure handler.
     *
     * @param mvc HTTP client
     * @param ctx Database context
     * @throws Exception If login fails
     */
    @Test
    void rejectsWrongFormPassword(@Autowired final MockMvc mvc, @Autowired final DSLContext ctx)
        throws Exception {
        Assertions.assertEquals(
            "/users/login?error=bad_credentials",
            mvc.perform(
                SecurityMockMvcRequestBuilders.formLogin()
                    .user(SecurityConfigTest.administrator(ctx).credentials().username())
                    .password("WrongPassword1!")
            ).andReturn().getResponse().getRedirectedUrl(),
            "Wrong password did not reach the login failure handler"
        );
    }

    /**
     * Unknown users must retain their distinct error without a fallback provider masking it.
     *
     * @param mvc HTTP client
     * @throws Exception If login fails
     */
    @Test
    void reportsUnknownFormUser(@Autowired final MockMvc mvc) throws Exception {
        Assertions.assertEquals(
            "/users/login?error=user_not_found",
            mvc.perform(
                SecurityMockMvcRequestBuilders.formLogin()
                    .user(UUID.randomUUID().toString()).password("ValidPassword1!")
            ).andReturn().getResponse().getRedirectedUrl(),
            "Unknown user was masked by a fallback provider"
        );
    }

    /**
     * Basic authentication must load database credentials and authorities.
     *
     * @param mvc HTTP client
     * @param ctx Database context
     * @throws Exception If the request fails
     */
    @Test
    void acceptsBasicAuthentication(@Autowired final MockMvc mvc, @Autowired final DSLContext ctx)
        throws Exception {
        Assertions.assertEquals(
            HttpStatus.OK.value(),
            mvc.perform(
                MockMvcRequestBuilders.get(
                    "/schools/{school}/schedules",
                    new SlsPostgres(ctx).create(
                        new SlInputName(new SlInput(), "Школа проверки доступа")
                    ).uid()
                ).accept("application/vnd.surok.schedules.v1+json").with(
                    SecurityMockMvcRequestPostProcessors.httpBasic(
                        SecurityConfigTest.administrator(ctx).credentials().username(),
                        "ValidPassword1!"
                    )
                )
            ).andReturn().getResponse().getStatus(),
            "Basic authentication did not load the administrator's credentials and roles"
        );
    }

    /**
     * Basic authentication must reject a wrong password.
     *
     * @param mvc HTTP client
     * @param ctx Database context
     * @throws Exception If the request fails
     */
    @Test
    void rejectsWrongBasicPassword(@Autowired final MockMvc mvc, @Autowired final DSLContext ctx)
        throws Exception {
        Assertions.assertEquals(
            HttpStatus.UNAUTHORIZED.value(),
            mvc.perform(
                MockMvcRequestBuilders.get("/schools/1/schedules")
                    .accept("application/vnd.surok.schedules.v1+json").with(
                    SecurityMockMvcRequestPostProcessors.httpBasic(
                        SecurityConfigTest.administrator(ctx).credentials().username(),
                        "WrongPassword1!"
                    )
                )
            ).andReturn().getResponse().getStatus(),
            "Basic authentication accepted an incorrect password"
        );
    }

    /**
     * Anonymous data requests must remain protected.
     *
     * @param accept Requested representation
     * @param mvc HTTP client
     * @throws Exception If the request fails
     */
    @ParameterizedTest
    @ValueSource(
        strings = {
            "application/vnd.surok.schedules.v1+json", "application/vnd.surok.schedules.v1+xml",
            "application/json", "application/xml"
        }
    )
    void rejectsAnonymousDataRequests(final String accept, @Autowired final MockMvc mvc)
        throws Exception {
        Assertions.assertEquals(
            HttpStatus.UNAUTHORIZED.value(),
            mvc.perform(
                MockMvcRequestBuilders.get("/schools/1/schedules")
                    .accept(accept)
            ).andReturn().getResponse().getStatus(),
            "Anonymous request bypassed resource authentication"
        );
    }

    /**
     * Browser requests must retain the existing login redirect.
     *
     * @param accept Requested representation
     * @param mvc HTTP client
     * @throws Exception If the request fails
     */
    @ParameterizedTest
    @ValueSource(strings = {"text/html", "*/*"})
    void redirectsAnonymousBrowserRequests(final String accept, @Autowired final MockMvc mvc)
        throws Exception {
        Assertions.assertEquals(
            "http://localhost/users/login",
            mvc.perform(
                MockMvcRequestBuilders.get("/schools/1/schedules").accept(accept)
            ).andReturn().getResponse().getRedirectedUrl(),
            "Browser request lost its login redirect"
        );
    }

    /**
     * Basic authentication must not bypass CSRF on the unified resource.
     *
     * @param mvc HTTP client
     * @param ctx Database context
     * @throws Exception If the request fails
     */
    @Test
    void protectsBasicMutationsWithCsrf(
        @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        Assertions.assertEquals(
            HttpStatus.FORBIDDEN.value(),
            mvc.perform(
                MockMvcRequestBuilders.post(
                    "/schools/{school}/schedules",
                    new SlsPostgres(ctx).create(
                        new SlInputName(new SlInput(), "Школа проверки CSRF")
                    ).uid()
                ).accept("application/vnd.surok.schedule-v1+json")
                    .contentType("application/vnd.surok.schedule-v1+json")
                    .content("{\"name\":\"Year\"}").with(
                        SecurityMockMvcRequestPostProcessors.httpBasic(
                            SecurityConfigTest.administrator(ctx).credentials().username(),
                            "ValidPassword1!"
                        )
                    )
            ).andReturn().getResponse().getStatus(),
            "Basic authentication bypassed CSRF protection"
        );
    }

    /**
     * OAuth2 login must still initiate the configured authorization flow.
     *
     * @param mvc HTTP client
     * @throws Exception If the request fails
     */
    @Test
    void startsYandexLogin(@Autowired final MockMvc mvc) throws Exception {
        MatcherAssert.assertThat(
            "OAuth2 login did not redirect to Yandex",
            mvc.perform(MockMvcRequestBuilders.get("/oauth2/authorization/yandex"))
                .andReturn().getResponse().getHeader(HttpHeaders.LOCATION),
            Matchers.startsWith("https://oauth.yandex.ru/authorize?")
        );
    }

    /**
     * API documentation must use the version of the built artifact.
     *
     * @param api OpenAPI definition
     */
    @Test
    void exposesBuildVersion(@Autowired final OpenAPI api) {
        Assertions.assertEquals(
            this.build.getVersion(), api.getInfo().getVersion(),
            "OpenAPI version differs from the built artifact"
        );
    }

    /**
     * XSLT pages must display the version of the built artifact.
     *
     * @param path Page path
     * @param mvc HTTP client
     * @param ctx Database context
     * @throws Exception If the page cannot be rendered
     */
    @ParameterizedTest
    @ValueSource(strings = {"/schools", "/federal/curriculums"})
    void displaysBuildVersion(
        final String path, @Autowired final MockMvc mvc, @Autowired final DSLContext ctx
    ) throws Exception {
        MatcherAssert.assertThat(
            "Page header version differs from the built artifact",
            mvc.perform(
                MockMvcRequestBuilders.get(path).accept(MediaType.TEXT_HTML).with(
                    SecurityMockMvcRequestPostProcessors.user(
                        new AuthUser(SecurityConfigTest.administrator(ctx))
                    )
                )
            ).andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString(),
            Matchers.containsString(
                String.format(
                    "class=\"app-header-logo-version\">v%s</span>", this.build.getVersion()
                )
            )
        );
    }

    /**
     * Connects Spring to the isolated database and applies migrations.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SecurityConfigTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", SecurityConfigTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", SecurityConfigTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }

    private static User administrator(final DSLContext ctx) throws Exception {
        final String name = UUID.randomUUID().toString();
        final User user = new UrsPostgres(ctx).register(
            name, "ValidPassword1!", String.format("%s@example.com", name), false
        );
        user.roles().grant(Roles.RoleEnum.ADMIN);
        return user;
    }
}
