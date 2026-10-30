/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.User;
import java.util.UUID;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Assertions;
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
 * Verifies parameterized user SQL against PostgreSQL.
 * For example, quoted usernames must remain data during identification.
 *
 * @since 0.0.1
 */
@Testcontainers
@DirtiesContext
@SpringBootTest
final class UrsPostgresTest {

    /** Isolated runtime-version database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:14-alpine");

    /**
     * Verifies lookup with quoted Unicode input.
     *
     * @param ctx Database context
     */
    @Test
    void identifiesQuotedUnicodeUsernames(@Autowired final DSLContext ctx) throws Exception {
        final String username = String.format("Ученик O'Brien '%s' OR '1'='1", UUID.randomUUID());
        Assertions.assertEquals(
            new UrsPostgres(ctx).register(
                username,
                "ValidPassword1!",
                String.format("%s@example.com", UUID.randomUUID()),
                false
            ).uid(),
            new UrsPostgres(ctx).identification(username).uid(),
            "User identification did not bind the supplied username"
        );
    }

    /**
     * Verifies that recent failures trigger the existing lockout rule.
     *
     * @param ctx Database context
     */
    @Test
    void rejectsUsersWithRecentFailedAttempts(@Autowired final DSLContext ctx) throws Exception {
        final User user = new UrsPostgres(ctx).register(
            String.format("Блокировка %s", UUID.randomUUID()),
            "ValidPassword1!", String.format("%s@example.com", UUID.randomUUID()), false
        );
        ctx.execute(
            """
                INSERT INTO public.login_attempt (user_id, time, success)
                SELECT ?, CURRENT_TIMESTAMP, false FROM generate_series(1, 6)
                """,
            user.uid()
        );
        Assertions.assertThrows(
            UserLockedException.class,
            () -> new UrsPostgres(ctx).authentication(user.credentials().username()),
            "Recent login failures did not lock the user"
        );
    }

    /**
     * Verifies that failures outside the lockout window are ignored.
     *
     * @param ctx Database context
     */
    @Test
    void acceptsUsersWithExpiredFailedAttempts(@Autowired final DSLContext ctx) throws Exception {
        final User user = new UrsPostgres(ctx).register(
            String.format("История %s", UUID.randomUUID()),
            "ValidPassword1!", String.format("%s@example.com", UUID.randomUUID()), false
        );
        ctx.execute(
            """
                INSERT INTO public.login_attempt (user_id, time, success)
                SELECT ?, CURRENT_TIMESTAMP - INTERVAL '10 minutes', false
                FROM generate_series(1, 6)
                """,
            user.uid()
        );
        Assertions.assertEquals(
            user.uid(),
            new UrsPostgres(ctx).authentication(user.credentials().username()).uid(),
            "Expired login failures incorrectly locked the user"
        );
    }

    /**
     * Verifies timestamp binding when a user is soft deleted.
     *
     * @param ctx Database context
     */
    @Test
    void excludesRemovedUsersFromIdentification(@Autowired final DSLContext ctx) throws Exception {
        final User user = new UrsPostgres(ctx).register(
            String.format("Удаление %s", UUID.randomUUID()),
            "ValidPassword1!", String.format("%s@example.com", UUID.randomUUID()), false
        );
        new UrsPostgres(ctx).remove(user.uid());
        Assertions.assertThrows(
            UserNotFoundException.class,
            () -> new UrsPostgres(ctx).identification(user.uid()),
            "Deleted user remained available for identification"
        );
    }

    /**
     * Connects Spring to the isolated database and applies migrations.
     *
     * @param registry Spring property registry
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", UrsPostgresTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", UrsPostgresTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", UrsPostgresTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
