/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.user.subscription.PgSubscriptions;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
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
 * Profile navigation and subscription forms verified through the security filter chain.
 *
 * @since 0.0.1
 * @checkstyle ProhibitFieldsInTestClassesCheck (1000 lines)
 */
@Testcontainers
@DirtiesContext
@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings({"PMD.UnitTestShouldIncludeAssert", "UnusedMethod"})
final class AccountHttpTest {

    /** Test database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16");

    /** Database context. */
    private final DSLContext ctx;

    /** HTTP client. */
    private final MockMvc mvc;

    @Autowired
    AccountHttpTest(final DSLContext ctx, final MockMvc mvc) {
        this.ctx = ctx;
        this.mvc = mvc;
    }

    @BeforeEach
    void insertSchoolContext() {
        this.ctx.execute(
            "INSERT INTO public.school (id, name) VALUES (1, 'Своя школа'), (2, 'Чужая школа')"
        );
        this.ctx.execute(
            """
            INSERT INTO public.schedule (id, school_id, name, is_deleted)
            VALUES (10, 1, 'Учебный год', false), (11, 1, 'Архив', true),
                (20, 2, 'Чужое расписание', false)
            """
        );
    }

    @AfterEach
    void truncateAccounts() {
        this.ctx.execute(
            "TRUNCATE TABLE public.school, subject, public.\"user\" RESTART IDENTITY CASCADE"
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"/users/profile", "/users/subscription"})
    void requiresAuthentication(final String path) throws Exception {
        this.mvc.perform(MockMvcRequestBuilders.get(path).accept(MediaType.TEXT_HTML))
            .andExpect(MockMvcResultMatchers.status().isFound()).andExpect(
                MockMvcResultMatchers.redirectedUrl("http://localhost/users/login")
            );
    }

    @Test
    void printsProfileAndSchoolNavigationWithoutSecrets() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/users/profile").accept(MediaType.TEXT_HTML).with(
                SecurityMockMvcRequestPostProcessors.user(this.user(false, 1L))
            )
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().string(
                Matchers.allOf(
                    Matchers.containsString("Информация об аккаунте"),
                    Matchers.containsString("STUDENT"),
                    Matchers.containsString("BASIC_MAKER"),
                    Matchers.containsString("href=\"/schools/1\""),
                    Matchers.containsString("href=\"/schools/1/schedules/10\""),
                    Matchers.containsString("href=\"/users/subscription\""),
                    Matchers.containsString("action=\"/logout\""),
                    Matchers.not(Matchers.containsString("Архив")),
                    Matchers.not(Matchers.containsString("Чужое расписание")),
                    Matchers.not(Matchers.containsString("ValidPassword1!")),
                    Matchers.not(Matchers.containsString("$2a$")),
                    Matchers.not(Matchers.containsString("th:"))
                )
            )
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void printsProfileWithoutSchool(final boolean admin) throws Exception {
        final AuthUser user = this.user(false, null);
        if (admin) {
            user.roles().grant(Roles.RoleEnum.ADMIN);
        }
        this.mvc.perform(
            MockMvcRequestBuilders.get("/users/profile").accept(MediaType.TEXT_HTML).with(
                SecurityMockMvcRequestPostProcessors.user(user)
            )
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().string(
                Matchers.allOf(
                    Matchers.containsString(user.getUsername()),
                    Matchers.not(Matchers.containsString("/schools//")),
                    Matchers.not(Matchers.containsString("href=\"/schools/1\""))
                )
            )
        );
    }

    @Test
    void printsEmptyRoles() throws Exception {
        final AuthUser user = this.user(false, 1L);
        this.ctx.execute("DELETE FROM public.user_role WHERE user_id = ?", user.uid());
        this.mvc.perform(
            MockMvcRequestBuilders.get("/users/profile").accept(MediaType.TEXT_HTML).with(
                SecurityMockMvcRequestPostProcessors.user(user)
            )
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().string(Matchers.containsString("Нет ролей"))
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void protectsLogout(final boolean protectedform) throws Exception {
        final AuthUser user = this.user(false, 1L);
        final MockHttpServletRequestBuilder request;
        if (protectedform) {
            request = this.form("/logout", user).accept(MediaType.TEXT_HTML);
        } else {
            request = MockMvcRequestBuilders.post("/logout")
                .with(SecurityMockMvcRequestPostProcessors.user(user));
        }
        final int status;
        if (protectedform) {
            status = 302;
        } else {
            status = 403;
        }
        this.mvc.perform(request).andExpect(MockMvcResultMatchers.status().is(status));
    }

    @Test
    void rendersBasicPlanWithBannerAndNoCancellation() throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/users/subscription").accept(MediaType.TEXT_HTML).with(
                SecurityMockMvcRequestPostProcessors.user(this.user(false, 1L))
            )
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().string(
                Matchers.allOf(
                    Matchers.containsString("Функционал в разработке"),
                    Matchers.containsString("Бессрочно"),
                    Matchers.containsString("Бесплатно"),
                    Matchers.containsString("name=\"plan\" type=\"hidden\" value=\"PRO\""),
                    Matchers.containsString("name=\"_csrf\""),
                    Matchers.not(Matchers.containsString("id=\"subscription-cancel\""))
                )
            )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADVANCED", "PRO", "VIEWER"})
    void rendersPaidPlanAndExpiry(final Subscription.Plan plan) throws Exception {
        final AuthUser user = this.user(false, 1L);
        new PgSubscriptions(this.ctx, user).subscription(plan);
        final OffsetDateTime expiry = OffsetDateTime.now(ZoneOffset.UTC).plusDays(10);
        this.ctx.execute(
            """
                UPDATE public.subscription SET expires_at = CAST(? AS timestamptz) WHERE user_id = ?
                """, expiry, user.uid()
        );
        this.mvc.perform(
            MockMvcRequestBuilders.get("/users/subscription").accept(MediaType.TEXT_HTML).with(
                SecurityMockMvcRequestPostProcessors.user(user)
            )
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().string(
                Matchers.allOf(
                    Matchers.containsString("Активна до"),
                    Matchers.containsString(
                        expiry.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                    ),
                    Matchers.containsString("id=\"subscription-cancel\""),
                    Matchers.not(Matchers.containsString("Истекла"))
                )
            )
        );
    }

    @Test
    void rendersExpiredSubscription() throws Exception {
        final AuthUser user = this.user(false, 1L);
        new PgSubscriptions(this.ctx, user).subscription(Subscription.Plan.PRO);
        this.ctx.execute(
            """
                UPDATE public.subscription SET expires_at = CAST(? AS timestamptz) WHERE user_id = ?
                """,
            OffsetDateTime.now(ZoneOffset.UTC).minusDays(1), user.uid()
        );
        this.mvc.perform(
            MockMvcRequestBuilders.get("/users/subscription").accept(MediaType.TEXT_HTML).with(
                SecurityMockMvcRequestPostProcessors.user(user)
            )
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().string(
                Matchers.allOf(
                    Matchers.containsString("Истекла"),
                    Matchers.not(Matchers.containsString("Активна до"))
                )
            )
        );
    }

    @ParameterizedTest
    @CsvSource(
        {
            "activated, Подписка активирована на 30 дней", "cancelled, Подписка отменена",
            "error, Произошла ошибка"
        }
    )
    void rendersStatusMessages(final String flag, final String message) throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.get("/users/subscription").accept(MediaType.TEXT_HTML)
                .param(flag, "").with(
                    SecurityMockMvcRequestPostProcessors.user(this.user(false, 1L))
                )
        ).andExpect(MockMvcResultMatchers.status().isOk()).andExpect(
            MockMvcResultMatchers.content().string(Matchers.containsString(message))
        );
    }

    @ParameterizedTest
    @CsvSource({"true, false", "false, true"})
    void redirectsRestrictedAccountsWithoutChangingData(
        final boolean corporate, final boolean admin
    ) throws Exception {
        final AuthUser user = this.user(corporate, 1L);
        if (admin) {
            user.roles().grant(Roles.RoleEnum.ADMIN);
        }
        this.mvc.perform(
            MockMvcRequestBuilders.get("/users/subscription").accept(MediaType.TEXT_HTML).with(
                SecurityMockMvcRequestPostProcessors.user(user)
            )
        ).andExpect(MockMvcResultMatchers.status().isFound())
            .andExpect(MockMvcResultMatchers.redirectedUrl("/users/profile"));
        for (final String suffix : List.of("checkout", "cancel")) {
            this.mvc.perform(
                this.form(String.format("/users/subscription/%s", suffix), user)
                    .param("plan", "PRO")
            ).andExpect(MockMvcResultMatchers.status().isFound())
                .andExpect(MockMvcResultMatchers.redirectedUrl("/users/profile"));
        }
        this.mvc.perform(
            MockMvcRequestBuilders.get("/users/profile").accept(MediaType.TEXT_HTML).with(
                SecurityMockMvcRequestPostProcessors.user(user)
            )
        ).andExpect(
            MockMvcResultMatchers.content().string(
                Matchers.not(Matchers.containsString("href=\"/users/subscription\""))
            )
        );
        MatcherAssert.assertThat(this.count(), Matchers.equalTo(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"application/json", "application/xml", "*/*;q=0", "invalid"})
    void rejectsUnsupportedAcceptBeforeSubscriptionChanges(final String accept) throws Exception {
        final AuthUser user = this.user(false, 1L);
        for (final String path : List.of("/users/profile", "/users/subscription")) {
            this.mvc.perform(
                MockMvcRequestBuilders.get(path).header("Accept", accept)
                    .with(SecurityMockMvcRequestPostProcessors.user(user))
            ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
        }
        for (final String suffix : List.of("checkout", "cancel")) {
            this.mvc.perform(
                this.form(String.format("/users/subscription/%s", suffix), user)
                    .param("plan", "PRO").header("Accept", accept)
            ).andExpect(MockMvcResultMatchers.status().isNotAcceptable());
        }
        MatcherAssert.assertThat(this.count(), Matchers.equalTo(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"checkout", "cancel"})
    void rejectsMutationsWithoutCsrf(final String suffix) throws Exception {
        this.mvc.perform(
            MockMvcRequestBuilders.post(String.format("/users/subscription/%s", suffix))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).param("plan", "PRO")
                .with(SecurityMockMvcRequestPostProcessors.user(this.user(false, 1L)))
        ).andExpect(MockMvcResultMatchers.status().isForbidden());
        MatcherAssert.assertThat(this.count(), Matchers.equalTo(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"checkout", "cancel"})
    void rejectsJsonFormBodies(final String suffix) throws Exception {
        this.mvc.perform(
            this.form(
                String.format("/users/subscription/%s", suffix), this.user(false, 1L)
            ).contentType(MediaType.APPLICATION_JSON).content("{}")
        ).andExpect(MockMvcResultMatchers.status().isUnsupportedMediaType());
        MatcherAssert.assertThat(this.count(), Matchers.equalTo(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"UNKNOWN", "", "pro"})
    void rejectsInvalidPlansWithoutMutation(final String plan) throws Exception {
        this.mvc.perform(
            this.form("/users/subscription/checkout", this.user(false, 1L)).param("plan", plan)
        ).andExpect(MockMvcResultMatchers.status().isBadRequest());
        MatcherAssert.assertThat(this.count(), Matchers.equalTo(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADVANCED", "PRO", "VIEWER"})
    void activatesPaidPlanAndUpdatesRoles(final Subscription.Plan plan) throws Exception {
        final AuthUser user = this.user(false, 1L);
        user.roles().grant(Roles.RoleEnum.TEACHER);
        this.mvc.perform(
            this.form("/users/subscription/checkout", user).param("plan", plan.name())
        ).andExpect(MockMvcResultMatchers.status().isFound()).andExpect(
            MockMvcResultMatchers.redirectedUrl(
                String.format("/users/subscription?activated=%s", plan.name())
            )
        );
        final Subscription current = new PgSubscriptions(this.ctx, user).subscription();
        MatcherAssert.assertThat(
            Map.of(
                "plan", current.plan(),
                "school", user.roles().list().stream().anyMatch(
                    role -> "TEACHER".equals(role.name())
                ),
                "valid", current.expiry().isAfter(OffsetDateTime.now(ZoneOffset.UTC).plusDays(29))
            ),
            Matchers.equalTo(Map.of("plan", plan, "school", true, "valid", true))
        );
    }

    @Test
    void cancelsPlanAndPreservesSchoolRoles() throws Exception {
        final AuthUser user = this.user(false, 1L);
        user.roles().grant(Roles.RoleEnum.TEACHER);
        final PgSubscriptions plans = new PgSubscriptions(this.ctx, user);
        plans.subscription(Subscription.Plan.PRO);
        this.mvc.perform(this.form("/users/subscription/cancel", user))
            .andExpect(MockMvcResultMatchers.status().isFound())
            .andExpect(MockMvcResultMatchers.redirectedUrl("/users/subscription?cancelled"));
        MatcherAssert.assertThat(
            Map.of(
                "plan", plans.subscription().plan(), "expiry", plans.subscription().expiry(),
                "roles", user.roles().list().stream().map(Role::name).sorted().toList()
            ),
            Matchers.equalTo(
                Map.of(
                    "plan", Subscription.Plan.BASIC, "expiry", OffsetDateTime.MAX,
                    "roles", List.of("BASIC_MAKER", "STUDENT", "TEACHER")
                )
            )
        );
    }

    @Test
    void keepsBasicCheckoutAsNoOp() throws Exception {
        final AuthUser user = this.user(false, 1L);
        final PgSubscriptions plans = new PgSubscriptions(this.ctx, user);
        plans.subscription(Subscription.Plan.PRO);
        this.mvc.perform(this.form("/users/subscription/checkout", user).param("plan", "BASIC"))
            .andExpect(MockMvcResultMatchers.status().isFound())
            .andExpect(MockMvcResultMatchers.redirectedUrl("/users/subscription"));
        MatcherAssert.assertThat(
            plans.subscription().plan(), Matchers.equalTo(Subscription.Plan.PRO)
        );
    }

    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", AccountHttpTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", AccountHttpTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", AccountHttpTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }

    private AuthUser user(final boolean corporate, final Long school) throws Exception {
        final String name = UUID.randomUUID().toString();
        return new AuthUser(
            new UrsPostgres(this.ctx, school).register(
                name, "ValidPassword1!", String.format("%s@example.com", name), corporate
            )
        );
    }

    private MockHttpServletRequestBuilder form(final String path, final AuthUser user)
        throws Exception {
        final MvcResult page = this.mvc.perform(
            MockMvcRequestBuilders.get("/users/profile").accept(MediaType.TEXT_HTML)
                .with(SecurityMockMvcRequestPostProcessors.user(user))
        ).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();
        final CsrfToken token = (CsrfToken) page.getRequest().getAttribute(
            CsrfToken.class.getName()
        );
        return MockMvcRequestBuilders.post(path)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .cookie(page.getResponse().getCookies())
            .param(token.getParameterName(), token.getToken())
            .with(SecurityMockMvcRequestPostProcessors.user(user));
    }

    private int count() {
        return this.ctx.fetchCount(DSL.table("public.subscription"));
    }
}
