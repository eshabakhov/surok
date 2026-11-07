/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.User;
import com.eshabakhov.surok.school.SlsPostgres;
import com.eshabakhov.surok.school.input.SlInput;
import com.eshabakhov.surok.school.input.SlInputName;
import java.util.List;
import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Public pages and native registration verified through the security filter chain.
 *
 * @since 0.0.1
 * @checkstyle ProhibitFieldsInTestClassesCheck (1000 lines)
 */
@Testcontainers
@DirtiesContext
@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings({"PMD.UnitTestShouldIncludeAssert", "UnusedMethod"})
final class AuthHttpTest {

    /** Test database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16");

    /** Database context. */
    private final DSLContext ctx;

    /** HTTP client. */
    private final MockMvc mvc;

    @Autowired
    AuthHttpTest(final DSLContext ctx, final MockMvc mvc) {
        this.ctx = ctx;
        this.mvc = mvc;
    }

    @AfterEach
    void truncateAccounts() {
        this.ctx.query(
            "TRUNCATE TABLE public.school, subject, public.\"user\" RESTART IDENTITY CASCADE"
        ).execute();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/users/login", "/users/register"})
    void rendersPublicPages(final String path) throws Exception {
        this.mvc.perform(MockMvcRequestBuilders.get(path).accept(MediaType.TEXT_HTML))
            .andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
                MockMvcResultMatchers.content().contentTypeCompatibleWith(MediaType.TEXT_HTML)
            ).andExpect(
                MockMvcResultMatchers.content().string(
                    Matchers.not(Matchers.containsString("th:"))
                )
            );
    }

    @ParameterizedTest
    @ValueSource(strings = {"/users/login", "/users/register"})
    void printsAnonymousCsrfToken(final String path) throws Exception {
        final MvcResult result = this.mvc.perform(
            MockMvcRequestBuilders.get(path).accept(MediaType.TEXT_HTML)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        final CsrfToken token = (CsrfToken) result.getRequest().getAttribute(
            CsrfToken.class.getName()
        );
        MatcherAssert.assertThat(
            result.getResponse().getContentAsString(),
            Matchers.allOf(
                Matchers.containsString(String.format("value=\"%s\"", token.getToken())),
                Matchers.containsString(
                    String.format("name=\"_csrf_header\" content=\"%s\"", token.getHeaderName())
                )
            )
        );
    }

    @Test
    void directsAnonymousVisitorsToLogin() throws Exception {
        this.mvc.perform(MockMvcRequestBuilders.get("/").accept(MediaType.TEXT_HTML)).andExpect(
            MockMvcResultMatchers.content().string(
                Matchers.containsString("class=\"cta\" href=\"/users/login\"")
            )
        );
    }

    @ParameterizedTest
    @CsvSource(
        {
            "ADMIN, false, /schools", "VIEWER, false, /schools", "DIRECTOR, true, /schools"
        }
    )
    void selectsAuthenticatedDestination(
        final Roles.RoleEnum role, final boolean school, final String destination
    ) throws Exception {
        Long identifier = null;
        if (school) {
            identifier = new SlsPostgres(this.ctx).create(
                new SlInputName(new SlInput(), "Школа")
            ).uid();
        }
        final User user = new UrsPostgres(this.ctx, identifier)
            .register("visitor", "ValidPassword1!", "visitor@example.com", false);
        if (school) {
            new UrsPostgres(this.ctx, identifier).roles(user).grant(role);
        } else if (role != Roles.RoleEnum.VIEWER) {
            user.roles().grant(role);
        }
        this.mvc.perform(
            MockMvcRequestBuilders.get("/").accept(MediaType.TEXT_HTML).with(
                SecurityMockMvcRequestPostProcessors.user(new AuthUser(user))
            )
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().string(
                Matchers.containsString(String.format("class=\"cta\" href=\"%s\"", destination))
            )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/json", "application/xml", "*/*;q=0", "invalid"})
    void rejectsUnsupportedRepresentations(final String accept) throws Exception {
        for (final String path : List.of("/", "/users/login", "/users/register")) {
            this.mvc.perform(MockMvcRequestBuilders.get(path).header("Accept", accept))
                .andExpect(MockMvcResultMatchers.status().isNotAcceptable());
        }
    }

    @Test
    void registersPersonalAccountAndAllowsLogin() throws Exception {
        this.mvc.perform(this.registration())
            .andExpect(MockMvcResultMatchers.status().isFound()).andExpect(
                MockMvcResultMatchers.redirectedUrl("/users/login?registered")
            );
        final User created = new UrsPostgres(this.ctx).identification("visitor");
        this.mvc.perform(
            SecurityMockMvcRequestBuilders.formLogin().user("visitor").password("ValidPassword1!")
        ).andExpect(MockMvcResultMatchers.status().isFound()).andExpect(
            MockMvcResultMatchers.redirectedUrl("/schools")
        );
        MatcherAssert.assertThat(
            Map.of("corporate", created.info().corporate(), "schools", this.schools()),
            Matchers.equalTo(Map.of("corporate", false, "schools", 0))
        );
    }

    @Test
    void rejectsRegistrationWithoutCsrf() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.post("/users/register")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("username", "visitor").param("email", "visitor@example.com")
                .param("password", "ValidPassword1!").param("confirmPassword", "ValidPassword1!")
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
        MatcherAssert.assertThat(this.schools(), Matchers.equalTo(0));
    }

    @ParameterizedTest
    @CsvSource(
        {
            "username, ab", "email, invalid", "password, weak", "confirmPassword, different"
        }
    )
    void rollsBackInvalidRegistration(final String field, final String value) throws Exception {
        this.mvc.perform(
            this.registration().with(AuthHttpTest.parameter(field, value))
        ).andExpect(MockMvcResultMatchers.status().isBadRequest()).andExpect(
            MockMvcResultMatchers.content().string(
                Matchers.allOf(
                    Matchers.containsString("id=\"error-message\" role=\"alert\""),
                    Matchers.containsString("action=\"/users/register\""),
                    Matchers.not(Matchers.containsString("value=\"ValidPassword1!\"")),
                    Matchers.not(Matchers.containsString("value=\"weak\""))
                )
            )
        );
        MatcherAssert.assertThat(this.schools(), Matchers.equalTo(0));
    }

    @Test
    void rejectsWeakMatchingPasswordsWithoutLeavingSchool() throws Exception {
        this.mvc.perform(
            this.registration().with(AuthHttpTest.parameter("password", "weak"))
                .with(AuthHttpTest.parameter("confirmPassword", "weak"))
        ).andExpect(MockMvcResultMatchers.status().isBadRequest());
        MatcherAssert.assertThat(this.schools(), Matchers.equalTo(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"username", "email"})
    void rejectsDuplicateRegistrationWithoutLeavingSchool(final String field) throws Exception {
        this.mvc.perform(this.registration()).andExpect(MockMvcResultMatchers.status().isFound());
        final MockHttpServletRequestBuilder request = this.registration();
        if ("username".equals(field)) {
            request.with(AuthHttpTest.parameter("email", "other@example.com"));
        } else {
            request.with(AuthHttpTest.parameter("username", "another"));
        }
        this.mvc.perform(request).andExpect(
            MockMvcResultMatchers.status().isBadRequest()
        ).andExpect(
            MockMvcResultMatchers.content().string(
                Matchers.containsString("Пользователь с таким логином или email уже существует")
            )
        );
        MatcherAssert.assertThat(this.schools(), Matchers.equalTo(0));
    }

    @Test
    void checksAcceptBeforeRegistration() throws Exception {
        this.mvc.perform(this.registration().accept(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isNotAcceptable());
        MatcherAssert.assertThat(this.schools(), Matchers.equalTo(0));
    }

    @Test
    void rejectsJsonInsteadOfForm() throws Exception {
        this.mvc.perform(
            this.registration().contentType(MediaType.APPLICATION_JSON).content("{}")
        ).andExpect(MockMvcResultMatchers.status().isUnsupportedMediaType());
        MatcherAssert.assertThat(this.schools(), Matchers.equalTo(0));
    }

    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", AuthHttpTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", AuthHttpTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", AuthHttpTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }

    private static RequestPostProcessor parameter(final String name, final String value) {
        return request -> {
            request.setParameter(name, value);
            return request;
        };
    }

    private MockHttpServletRequestBuilder registration() throws Exception {
        final MvcResult form = this.mvc.perform(
            MockMvcRequestBuilders.get("/users/register").accept(MediaType.TEXT_HTML)
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        final CsrfToken token = (CsrfToken) form.getRequest().getAttribute(
            CsrfToken.class.getName()
        );
        return MockMvcRequestBuilders.post("/users/register")
            .accept(MediaType.TEXT_HTML).contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .cookie(form.getResponse().getCookies())
            .param(token.getParameterName(), token.getToken())
            .param("username", "visitor").param("email", "visitor@example.com")
            .param("password", "ValidPassword1!").param("confirmPassword", "ValidPassword1!");
    }

    private int schools() {
        return this.ctx.fetchCount(DSL.table("public.school"));
    }
}
