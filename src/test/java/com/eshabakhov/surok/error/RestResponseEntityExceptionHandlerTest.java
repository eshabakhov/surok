/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.error;

import com.eshabakhov.surok.User;
import com.eshabakhov.surok.user.AuthUser;
import com.eshabakhov.surok.user.Roles;
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Domain, framework and security errors use the same representation and status.
 *
 * @since 0.0.1
 * @checkstyle ProhibitFieldsInTestClassesCheck (1000 lines)
 */
@Testcontainers
@DirtiesContext
@SpringBootTest(
    properties = {
        "app.security.docs.enabled=true", "logging.level.com.eshabakhov.surok.error=OFF"
    }
)
@AutoConfigureMockMvc
@SuppressWarnings("PMD.UnitTestShouldIncludeAssert")
final class RestResponseEntityExceptionHandlerTest {

    /** Test database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16");

    /** HTTP client. */
    private final MockMvc mvc;

    /** Database context. */
    private final DSLContext ctx;

    @Autowired
    RestResponseEntityExceptionHandlerTest(final MockMvc mvc, final DSLContext ctx) {
        this.mvc = mvc;
        this.ctx = ctx;
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "/schools/999999999", "/federal/curriculums/999999999",
            "/css/missing.css", "/несуществующий-ресурс"
        }
    )
    void rendersMissingResources(final String path) throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get(path).accept(MediaType.TEXT_HTML).with(
                SecurityMockMvcRequestPostProcessors.user(this.administrator())
            )
        ).andExpect(MockMvcResultMatchers.status().isNotFound()).andExpect(
            MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.TEXT_HTML)
        ).andExpect(RestResponseEntityExceptionHandlerTest.page(404));
    }

    @Test
    void preservesFormValidationRedirects() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.post("/schools").accept(MediaType.TEXT_HTML)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).param("name", "")
                .with(SecurityMockMvcRequestPostProcessors.csrf()).with(
                    SecurityMockMvcRequestPostProcessors.user(this.administrator())
                )
        ).andExpect(MockMvcResultMatchers.status().isFound())
            .andExpect(MockMvcResultMatchers.redirectedUrl("/schools/create?error=empty"));
    }

    @Test
    void rendersMalformedDocuments() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.post("/schools").accept(MediaType.TEXT_HTML)
                .contentType(MediaType.APPLICATION_JSON).content("{")
                .with(SecurityMockMvcRequestPostProcessors.csrf()).with(
                    SecurityMockMvcRequestPostProcessors.user(this.administrator())
                )
        ).andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(RestResponseEntityExceptionHandlerTest.page(400));
    }

    @Test
    void writesValidationErrorsAsData() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.post("/schools")
                .accept("application/vnd.surok.school-v1+json")
                .contentType("application/vnd.surok.school-v1+json").content("{\"name\":\"\"}")
                .with(SecurityMockMvcRequestPostProcessors.csrf()).with(
                    SecurityMockMvcRequestPostProcessors.user(this.administrator())
                )
        ).andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").isNotEmpty());
    }

    @Test
    void preservesAllowedMethodsInFrameworkErrors() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.put("/users/login").accept(MediaType.TEXT_HTML)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(MockMvcResultMatchers.status().isMethodNotAllowed()).andExpect(
            MockMvcResultMatchers.header().string("Allow", Matchers.containsString("GET"))
        ).andExpect(RestResponseEntityExceptionHandlerTest.page(405));
    }

    @Test
    void rendersUnsupportedRequestFormat() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.post("/users/register").accept(MediaType.TEXT_HTML)
                .contentType(MediaType.APPLICATION_JSON).content("{}")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(MockMvcResultMatchers.status().isUnsupportedMediaType())
            .andExpect(RestResponseEntityExceptionHandlerTest.page(415));
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/json", "application/vnd.surok.buildings.v1+json"})
    void writesJsonAuthenticationErrors(final String accept) throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/1/buildings").accept(accept)
        ).andExpect(MockMvcResultMatchers.status().isUnauthorized()).andExpect(
            MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        ).andExpect(
            MockMvcResultMatchers.header().string("WWW-Authenticate", "Basic realm=\"Surok\"")
        ).andExpect(MockMvcResultMatchers.jsonPath("$.status").value(401))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").isNotEmpty())
            .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void writesXmlAuthenticationErrorsForLegacyApi() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/api/users").accept(MediaType.APPLICATION_XML)
        ).andExpect(MockMvcResultMatchers.status().isUnauthorized()).andExpect(
            MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.APPLICATION_XML)
        ).andExpect(MockMvcResultMatchers.xpath("/error/status").string("401"));
    }

    @Test
    void rendersCsrfErrorsWithoutRedirecting() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.post("/users/register").accept(MediaType.TEXT_HTML)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        ).andExpect(MockMvcResultMatchers.status().isForbidden())
            .andExpect(RestResponseEntityExceptionHandlerTest.page(403));
    }

    @Test
    void rendersAuthorizationErrors() throws Exception {
        final User actor = new UrsPostgres(this.ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        this.mvc.perform(
            MockMvcRequestBuilders.get("/api/users/{user}/roles", actor.uid())
                .accept(MediaType.TEXT_HTML).with(
                    SecurityMockMvcRequestPostProcessors.user(new AuthUser(actor))
                )
        ).andExpect(MockMvcResultMatchers.status().isForbidden())
            .andExpect(RestResponseEntityExceptionHandlerTest.page(403));
    }

    @Test
    void leavesErrorPathUnmapped() throws Exception {
        MatcherAssert.assertThat(
            "The error path still exposed a dedicated endpoint",
            this.mvc.perform(
                MockMvcRequestBuilders.get("/error").accept(MediaType.APPLICATION_JSON).with(
                    SecurityMockMvcRequestPostProcessors.user("читатель").roles("ADMIN")
                )
            ).andReturn().getResponse().getStatus(),
            Matchers.is(404)
        );
    }

    @Test
    void omitsErrorOperationsFromApiDocumentation() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/api-docs").with(
                SecurityMockMvcRequestPostProcessors.user("читатель").roles("ADMIN")
            )
        ).andExpect(
            MockMvcResultMatchers.jsonPath("$.paths", Matchers.not(Matchers.hasKey("/error")))
        );
    }

    @ParameterizedTest
    @CsvSource(
        {
            "text/html,text/html", "application/vnd.surok.school-v1+json,application/json",
            "application/vnd.surok.school-v1+xml,application/xml"
        }
    )
    void returnsUnexpectedFailuresInTheOriginalResponse(final String accept, final String type)
        throws Exception {
        MatcherAssert.assertThat(
            "An unexpected database failure escaped the resource response or exposed its details",
            this.mvc.perform(
                MockMvcRequestBuilders.post("/schools").accept(accept)
                    .contentType("application/vnd.surok.school-v1+json")
                    .content("{\"name\":\"Секретная\\u0000школа\"}")
                    .with(SecurityMockMvcRequestPostProcessors.csrf()).with(
                        SecurityMockMvcRequestPostProcessors.user(this.administrator())
                    )
            ).andReturn().getResponse(),
            Matchers.allOf(
                Matchers.hasProperty("status", Matchers.is(500)),
                Matchers.hasProperty("contentType", Matchers.startsWith(type)),
                Matchers.hasProperty("forwardedUrl", Matchers.nullValue()),
                Matchers.hasProperty("redirectedUrl", Matchers.nullValue()),
                Matchers.hasProperty(
                    "contentAsString", Matchers.allOf(
                        Matchers.containsString("500"),
                        Matchers.not(Matchers.containsString("Секретная")),
                        Matchers.not(Matchers.containsString("SQL"))
                    )
                )
            )
        );
    }

    @Test
    void preservesSpecificUserExceptionHandling() throws Exception {
        MatcherAssert.assertThat(
            "The generic exception handler replaced the user validation response",
            this.mvc.perform(
                MockMvcRequestBuilders.post("/api/users").accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON).content("{}").with(
                        SecurityMockMvcRequestPostProcessors.user("читатель").roles("ADMIN")
                    )
            ).andReturn().getResponse().getStatus(),
            Matchers.is(400)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = "application/vnd.surok.school-v1+json")
    void writesDomainErrorsAsData(final String accept) throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/schools/999999999").accept(accept).with(
                SecurityMockMvcRequestPostProcessors.user(this.administrator())
            )
        ).andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(404));
    }

    /**
     * Connect the application to the test database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add(
            "spring.datasource.url", RestResponseEntityExceptionHandlerTest.POSTGRES::getJdbcUrl
        );
        registry.add(
            "spring.datasource.username", RestResponseEntityExceptionHandlerTest.POSTGRES::getUsername
        );
        registry.add(
            "spring.datasource.password", RestResponseEntityExceptionHandlerTest.POSTGRES::getPassword
        );
        registry.add("spring.liquibase.enabled", () -> true);
    }

    private static ResultMatcher page(final int status) {
        return MockMvcResultMatchers.content().string(
            Matchers.containsString(String.format("<title>%d - Surok</title>", status))
        );
    }

    private AuthUser administrator() throws Exception {
        final String username = UUID.randomUUID().toString();
        final User user = new UrsPostgres(this.ctx).register(
            username, "ValidPassword1!", String.format("%s@example.com", username), false
        );
        user.roles().grant(Roles.RoleEnum.ADMIN);
        return new AuthUser(user);
    }
}
