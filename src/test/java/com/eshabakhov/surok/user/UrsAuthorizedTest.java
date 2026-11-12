/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.Users;
import com.eshabakhov.surok.school.SlsPostgres;
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
 * School user operations follow the actor's current school role.
 * For example, a director can add a member until the director role is revoked.
 *
 * @since 0.0.1
 */
@Testcontainers
@SpringBootTest
final class UrsAuthorizedTest {

    /** Isolated database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    /**
     * Adds an existing user to the director's school.
     *
     * @param ctx Database context
     * @throws Exception If setup or addition fails
     */
    @Test
    void addsAMemberForTheSchoolDirector(@Autowired final DSLContext ctx) throws Exception {
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
        new UrsAuthorized(school.users(), school, actor).add(target);
        MatcherAssert.assertThat(
            "A school director could not add an existing member",
            school.users().contains(target), Matchers.is(true)
        );
    }

    /**
     * Lets a viewer inspect other members of their school.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void readsSchoolMembersForAViewer(@Autowired final DSLContext ctx) throws Exception {
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
            "A school viewer could not inspect another member",
            new UrsAuthorized(school.users(), school, viewer).contains(actor),
            Matchers.is(true)
        );
    }

    /**
     * Lets a global administrator add a member without school membership.
     *
     * @param ctx Database context
     * @throws Exception If setup or addition fails
     */
    @Test
    void addsAMemberForAnAdministratorOutsideTheSchool(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        actor.roles().grant(Roles.RoleEnum.ADMIN);
        final School school = new SlsPostgres(ctx).create(
            new SlInputName(new SlInput(), String.format("Чужая школа %s", UUID.randomUUID()))
        );
        final User target = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        new UrsAuthorized(school.users(), school, actor).add(target);
        MatcherAssert.assertThat(
            "A global administrator could not add a member without school membership",
            school.users().contains(target), Matchers.is(true)
        );
    }

    /**
     * Denies addition through a retained collection after director revocation.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void deniesMemberAdditionAfterDirectorRevocation(@Autowired final DSLContext ctx)
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
        final Users members = new UrsAuthorized(school.users(), school, actor);
        school.users().roles(actor).revoke(school.users().roles(actor).list().getFirst().uid());
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> members.add(target),
            "A former director added a member through a retained collection"
        );
    }

    /**
     * Configures the isolated database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", UrsAuthorizedTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", UrsAuthorizedTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", UrsAuthorizedTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
