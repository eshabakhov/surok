/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.User;
import com.eshabakhov.surok.school.SlsPostgres;
import com.eshabakhov.surok.school.input.SlInput;
import com.eshabakhov.surok.school.input.SlInputName;
import java.util.List;
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
            () -> new UrsPostgres(ctx).authentication(user.account().username()),
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
            new UrsPostgres(ctx).authentication(user.account().username()).uid(),
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
     * A user may join two schools with separate roles and leave one school.
     *
     * @param ctx Database context
     * @throws Exception If persistence fails
     */
    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void keepsMembershipsAndSchoolRolesIndependent(@Autowired final DSLContext ctx)
        throws Exception {
        final User user = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        Assertions.assertFalse(user.schools().iterate().iterator().hasNext());
        final long first = new SlsPostgres(ctx).create(
            new SlInputName(new SlInput(), "Первая школа")
        ).uid();
        final long second = new SlsPostgres(ctx).create(
            new SlInputName(new SlInput(), "Вторая школа")
        ).uid();
        final Roles initial = new UrsPostgres(ctx, first).roles(user);
        new UrsPostgres(ctx, first).add(user);
        new UrsPostgres(ctx, second).add(user);
        initial.grant(Roles.RoleEnum.DIRECTOR);
        new UrsPostgres(ctx, second).roles(user).grant(Roles.RoleEnum.TEACHER);
        Assertions.assertEquals(
            List.of("DIRECTOR"),
            initial.list().stream().map(Role::name).toList()
        );
        Assertions.assertEquals(
            List.of("TEACHER"),
            new UrsPostgres(ctx, second).roles(user).list().stream().map(Role::name).toList()
        );
        new UrsPostgres(ctx, first).remove(user.uid());
        Assertions.assertFalse(user.schools().contains(first));
        Assertions.assertTrue(user.schools().contains(second));
        Assertions.assertEquals(
            user.uid(), new UrsPostgres(ctx).identification(user.uid()).uid()
        );
    }

    /**
     * Creating a school through a user makes that user its director.
     *
     * @param ctx Database context
     * @throws Exception If creation fails
     */
    @Test
    void appointsSchoolCreatorAsDirector(@Autowired final DSLContext ctx) throws Exception {
        final User creator = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        Assertions.assertTrue(
            new UrsPostgres(
                ctx, creator.schools().create(
                    new SlInputName(new SlInput(), "Школа создателя")
                ).uid()
            ).roles(creator).contains(Roles.RoleEnum.DIRECTOR),
            "Creator was not assigned director in the new school"
        );
    }

    /**
     * Viewer is a school role and cannot be granted globally.
     *
     * @param ctx Database context
     * @throws Exception If user registration fails
     */
    @Test
    void rejectsGlobalViewerRole(@Autowired final DSLContext ctx) throws Exception {
        final User user = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        Assertions.assertThrows(
            RoleGrantException.class, () -> user.roles().grant(Roles.RoleEnum.VIEWER)
        );
    }

    /**
     * Deleted accounts cannot remain enabled through a retained security adapter.
     *
     * @param ctx Database context
     * @throws Exception If account creation or deletion fails
     */
    @Test
    void disablesADeletedAccountInTheSecurityAdapter(@Autowired final DSLContext ctx)
        throws Exception {
        final User user = new UrsPostgres(ctx).register(
            String.format("Удалённый-%s", UUID.randomUUID()), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        new UrsPostgres(ctx).remove(user.uid());
        Assertions.assertFalse(
            new AuthUser(user).isEnabled(), "A deleted account remained enabled for authentication"
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
