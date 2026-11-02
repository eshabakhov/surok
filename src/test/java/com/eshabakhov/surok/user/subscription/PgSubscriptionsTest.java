/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user.subscription;

import com.eshabakhov.surok.User;
import com.eshabakhov.surok.user.AuthUser;
import com.eshabakhov.surok.user.ExpiryReset;
import com.eshabakhov.surok.user.Role;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.Subscription;
import com.eshabakhov.surok.user.UrsPostgres;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verifies subscription SQL against PostgreSQL, including enum roles and expiry.
 * For example, upgrading a personal user must preserve their school roles.
 *
 * @since 0.0.1
 */
@Testcontainers
@DirtiesContext
@SpringBootTest
final class PgSubscriptionsTest {

    /** Isolated database using the application's runtime PostgreSQL version. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:14-alpine");

    /**
     * Verifies that upgrading only replaces subscription-managed roles.
     *
     * @param ctx Database context
     */
    @Test
    void preservesSchoolRolesWhenUpgrading(@Autowired final DSLContext ctx) throws Exception {
        final User user = new UrsPostgres(ctx).register(
            String.format("Ученик O'Brien %s", UUID.randomUUID()),
            "ValidPassword1!",
            String.format("upgrade-%s@example.com", UUID.randomUUID()),
            false
        );
        user.roles().grant(Roles.RoleEnum.TEACHER);
        new PgSubscriptions(ctx, new AuthUser(user))
            .subscription(Subscription.Plan.PRO);
        MatcherAssert.assertThat(
            "Upgraded subscription lost a role or retained an incorrect role",
            user.roles().list().stream().map(Role::name).toList(),
            Matchers.containsInAnyOrder(
                "STUDENT", "TEACHER", "BASIC_MAKER", "ADVANCED_MAKER", "PRO_MAKER"
            )
        );
    }

    /**
     * Verifies timestamp decoding for the expiration of a paid plan.
     *
     * @param ctx Database context
     */
    @Test
    void retainsPaidSubscriptionExpiry(@Autowired final DSLContext ctx) throws Exception {
        final PgSubscriptions plans = new PgSubscriptions(
            ctx,
            new AuthUser(
                new UrsPostgres(ctx).register(
                    String.format("План %s", UUID.randomUUID()),
                    "ValidPassword1!",
                    String.format("expiry-%s@example.com", UUID.randomUUID()),
                    false
                )
            )
        );
        plans.subscription(Subscription.Plan.ADVANCED);
        MatcherAssert.assertThat(
            "Paid subscription expires before its expected term",
            plans.subscription().expiry(),
            Matchers.greaterThan(OffsetDateTime.now(ZoneOffset.UTC).plusDays(29))
        );
    }

    /**
     * Verifies the upsert path and the null expiry of a perpetual plan.
     *
     * @param ctx Database context
     */
    @Test
    void makesDowngradedSubscriptionsPerpetual(@Autowired final DSLContext ctx) throws Exception {
        final PgSubscriptions plans = new PgSubscriptions(
            ctx,
            new AuthUser(
                new UrsPostgres(ctx).register(
                    String.format("Лицей %s", UUID.randomUUID()),
                    "ValidPassword1!",
                    String.format("downgrade-%s@example.com", UUID.randomUUID()),
                    false
                )
            )
        );
        plans.subscription(Subscription.Plan.PRO);
        plans.subscription(Subscription.Plan.BASIC);
        MatcherAssert.assertThat(
            "Downgraded subscription retained its paid plan or expiry",
            List.of(plans.subscription().plan(), plans.subscription().expiry()),
            Matchers.is(List.of(Subscription.Plan.BASIC, OffsetDateTime.MAX))
        );
    }

    /**
     * Verifies that expiry uses timestamp comparisons and restores basic roles.
     *
     * @param ctx Database context
     */
    @Test
    void restoresBasicRolesAfterSubscriptionExpiry(
        @Autowired final DSLContext ctx
    ) throws Exception {
        final User user = new UrsPostgres(ctx).register(
            String.format("Просмотрщик %s", UUID.randomUUID()),
            "ValidPassword1!",
            String.format("expired-%s@example.com", UUID.randomUUID()),
            false
        );
        new PgSubscriptions(ctx, new AuthUser(user))
            .subscription(Subscription.Plan.VIEWER);
        ctx.execute(
            "UPDATE public.subscription SET expires_at = CAST(? AS timestamptz) WHERE user_id = ?",
            OffsetDateTime.now(ZoneOffset.UTC).minusDays(2),
            user.uid()
        );
        new ExpiryReset(ctx).reset();
        MatcherAssert.assertThat(
            "Expired subscription retained its paid roles",
            user.roles().list().stream().map(Role::name).toList(),
            Matchers.containsInAnyOrder("STUDENT", "BASIC_MAKER")
        );
    }

    /**
     * Connects Spring to the isolated database.
     *
     * @param registry Spring property registry
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PgSubscriptionsTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", PgSubscriptionsTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", PgSubscriptionsTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
