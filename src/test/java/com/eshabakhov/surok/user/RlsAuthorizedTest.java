/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.school.input.SlInput;
import com.eshabakhov.surok.school.input.SlInputName;
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
 * School role assignments follow the actor's current school role.
 * For example, a viewer can inspect roles while only a director can grant them.
 *
 * @since 0.0.1
 */
@Testcontainers
@SpringBootTest
final class RlsAuthorizedTest {

    /** Isolated database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    /**
     * Grants a school role for its director.
     *
     * @param ctx Database context
     * @throws Exception If setup or assignment fails
     */
    @Test
    void grantsASchoolRoleForTheDirector(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = actor.schools().create(
            new SlInputName(new SlInput(), String.format("Школа %s", UUID.randomUUID()))
        );
        final User target = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        school.users().add(target);
        new RlsAuthorized(school.users().roles(target), school, actor).grant(
            Roles.RoleEnum.TEACHER
        );
        MatcherAssert.assertThat(
            "A school director could not grant a member role",
            school.users().roles(target).contains(Roles.RoleEnum.TEACHER),
            Matchers.is(true)
        );
    }

    /**
     * Lets a viewer inspect school roles.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void readsSchoolRolesForAViewer(@Autowired final DSLContext ctx) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = actor.schools().create(
            new SlInputName(new SlInput(), String.format("Гимназия %s", UUID.randomUUID()))
        );
        final User viewer = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        school.users().add(viewer);
        school.users().roles(viewer).grant(Roles.RoleEnum.VIEWER);
        MatcherAssert.assertThat(
            "A school viewer could not inspect their role",
            new RlsAuthorized(school.users().roles(viewer), school, viewer).contains(
                Roles.RoleEnum.VIEWER
            ), Matchers.is(true)
        );
    }

    /**
     * Prevents a school viewer from granting roles to another member.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void deniesRoleAssignmentByAViewer(@Autowired final DSLContext ctx) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = actor.schools().create(
            new SlInputName(new SlInput(), String.format("Школа %s", UUID.randomUUID()))
        );
        final User viewer = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final User target = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        school.users().add(viewer);
        school.users().roles(viewer).grant(Roles.RoleEnum.VIEWER);
        school.users().add(target);
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> new RlsAuthorized(school.users().roles(target), school, viewer).grant(
                Roles.RoleEnum.TEACHER
            ),
            "A school viewer granted a role to another member"
        );
    }

    /**
     * Denies assignment through retained roles after director revocation.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void deniesRoleAssignmentAfterDirectorRevocation(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = actor.schools().create(
            new SlInputName(new SlInput(), String.format("Лицей %s", UUID.randomUUID()))
        );
        final User target = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        school.users().add(target);
        final Roles roles = new RlsAuthorized(school.users().roles(target), school, actor);
        school.users().roles(actor).revoke(school.users().roles(actor).list().getFirst().uid());
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> roles.grant(Roles.RoleEnum.TEACHER),
            "A former director granted a role through retained school roles"
        );
    }

    /**
     * Configures the isolated database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", RlsAuthorizedTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", RlsAuthorizedTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", RlsAuthorizedTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
