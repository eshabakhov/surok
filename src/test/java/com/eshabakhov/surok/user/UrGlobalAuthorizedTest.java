/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.User;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Global user details depend on the actor's current administrator right.
 * For example, a retained user becomes inaccessible after the role is revoked.
 *
 * @since 0.0.1
 */
@Testcontainers
@SpringBootTest
final class UrGlobalAuthorizedTest {

    /** Isolated database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    /**
     * Reads another user's credentials for an administrator.
     *
     * @param ctx Database context
     * @throws Exception If registration or role assignment fails
     */
    @Test
    void readsGlobalUserCredentialsForAnAdministrator(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        actor.roles().grant(Roles.RoleEnum.ADMIN);
        final String name = String.format("пользователь-%s", UUID.randomUUID());
        MatcherAssert.assertThat(
            "An administrator could not read global user credentials",
            new UrGlobalAuthorized(
                new UrsPostgres(ctx).register(
                    name, "ValidPassword1!",
                    String.format("%s@example.com", UUID.randomUUID()), false
                ), actor
            ).credentials().username(), Matchers.is(name)
        );
    }

    /**
     * Rejects access to a retained user after administrator revocation.
     *
     * @param ctx Database context
     * @throws Exception If registration or role changes fail
     */
    @Test
    void rejectsRetainedUserAfterAdministratorRevocation(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final User target = new UrGlobalAuthorized(
            new UrsPostgres(ctx).register(
                String.format("цель-%s", UUID.randomUUID()), "ValidPassword1!",
                String.format("%s@example.com", UUID.randomUUID()), false
            ), actor
        );
        actor.roles().revoke(actor.roles().grant(Roles.RoleEnum.ADMIN).uid());
        Assertions.assertThrows(
            AccessDeniedException.class,
            target::credentials,
            "A former administrator accessed a retained global user"
        );
    }

    /**
     * Configures the isolated database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", UrGlobalAuthorizedTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", UrGlobalAuthorizedTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", UrGlobalAuthorizedTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
