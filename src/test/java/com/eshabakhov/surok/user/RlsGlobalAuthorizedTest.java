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
 * Global role operations use the actor's current administrator right.
 * For example, a retained role collection refuses changes after revocation.
 *
 * @since 0.0.1
 */
@Testcontainers
@SpringBootTest
final class RlsGlobalAuthorizedTest {

    /** Isolated database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    /**
     * Denies global role assignment by a regular user before accessing the target.
     *
     * @param ctx Database context
     * @throws Exception If registration fails
     */
    @Test
    void deniesRoleAssignmentByARegularUser(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            String.format("читатель-%s", UUID.randomUUID()), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> new RlsGlobalAuthorized(
                new Roles.RlsPostgres(ctx, 97L), actor
            ).grant(Roles.RoleEnum.PRO_MAKER),
            "A regular user assigned a global role"
        );
    }

    /**
     * Assigns and reads a global role for an administrator.
     *
     * @param ctx Database context
     * @throws Exception If registration or role assignment fails
     */
    @Test
    void assignsAndReadsAGlobalRoleForAnAdministrator(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            String.format("администратор-%s", UUID.randomUUID()), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final User target = new UrsPostgres(ctx).register(
            String.format("пользователь-%s", UUID.randomUUID()), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        actor.roles().grant(Roles.RoleEnum.ADMIN);
        final Roles roles = new RlsGlobalAuthorized(target.roles(), actor);
        roles.grant(Roles.RoleEnum.ADVANCED_MAKER);
        MatcherAssert.assertThat(
            "An administrator could not assign and read a global role",
            roles.contains(Roles.RoleEnum.ADVANCED_MAKER), Matchers.is(true)
        );
    }

    /**
     * Denies listing through a retained collection after administrator revocation.
     *
     * @param ctx Database context
     * @throws Exception If registration or role changes fail
     */
    @Test
    void deniesRoleListingAfterAdministratorRevocation(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            String.format("бывший-администратор-%s", UUID.randomUUID()), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final Roles roles = new RlsGlobalAuthorized(actor.roles(), actor);
        actor.roles().revoke(actor.roles().grant(Roles.RoleEnum.ADMIN).uid());
        Assertions.assertThrows(
            AccessDeniedException.class,
            roles::list,
            "A former administrator listed global roles through a retained collection"
        );
    }

    /**
     * Configures the isolated database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", RlsGlobalAuthorizedTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", RlsGlobalAuthorizedTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", RlsGlobalAuthorizedTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
