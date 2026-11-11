/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.User;
import com.eshabakhov.surok.Users;
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
 * Global user administration checks the actor's current administrator role.
 * For example, a retained role collection cannot be changed after revocation.
 *
 * @since 0.0.1
 */
@Testcontainers
@SpringBootTest
final class UrsGlobalAuthorizedTest {

    /** Isolated database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    /**
     * Rejects removal before looking up an unavailable user.
     *
     * @param ctx Database context
     * @throws Exception If registration fails
     */
    @Test
    void rejectsRemovalByARegularUser(@Autowired final DSLContext ctx) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> new UrsGlobalAuthorized(new UrsPostgres(ctx), actor).remove(97L),
            "A regular user removed an account"
        );
    }

    /**
     * Rejects role assignment through a retained user after administrator revocation.
     *
     * @param ctx Database context
     * @throws Exception If registration or role changes fail
     */
    @Test
    void rejectsRoleAssignmentAfterAdministratorRevocation(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final User target = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final Role granted = actor.roles().grant(Roles.RoleEnum.ADMIN);
        final Users users = new UrsGlobalAuthorized(new UrsPostgres(ctx), actor);
        final Roles roles = users.identification(target.uid()).roles();
        actor.roles().revoke(granted.uid());
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> roles.grant(Roles.RoleEnum.PRO_MAKER),
            "A former administrator assigned a role through a retained user"
        );
    }

    /**
     * Rejects role listing before looking up an unavailable user.
     *
     * @param ctx Database context
     * @throws Exception If registration fails
     */
    @Test
    void rejectsIdentificationByARegularUser(@Autowired final DSLContext ctx) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> new UrsGlobalAuthorized(new UrsPostgres(ctx), actor).identification(97L),
            "A regular user looked up a global account"
        );
    }

    /**
     * Registers a global user for an administrator and protects the returned user.
     *
     * @param ctx Database context
     * @throws Exception If registration or role assignment fails
     */
    @Test
    void registersAGlobalUserForAnAdministrator(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        actor.roles().grant(Roles.RoleEnum.ADMIN);
        final String name = String.format("новый-%s", UUID.randomUUID());
        MatcherAssert.assertThat(
            "An administrator could not register a global user",
            new UrsGlobalAuthorized(new UrsPostgres(ctx), actor).register(
                name, "ValidPassword1!",
                String.format("%s@example.com", UUID.randomUUID()), false
            ).credentials().username(), Matchers.is(name)
        );
    }

    /**
     * Configures the isolated database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", UrsGlobalAuthorizedTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", UrsGlobalAuthorizedTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", UrsGlobalAuthorizedTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
