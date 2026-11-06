/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.Users;
import com.eshabakhov.surok.school.building.input.BdInput;
import com.eshabakhov.surok.school.building.input.BdInputName;
import com.eshabakhov.surok.school.input.SlInput;
import com.eshabakhov.surok.school.input.SlInputName;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrsPostgres;
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
 * Checks modification rights on a protected school.
 * For example, a school viewer may inspect but not rename its school.
 *
 * @since 0.0.1
 */
@Testcontainers
@SpringBootTest
final class SlAuthorizedTest {

    /** Isolated database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    /**
     * Allows the school director to revise its school.
     *
     * @param ctx Database context
     * @throws Exception If school revision fails
     */
    @Test
    void allowsTheDirectorToReviseItsSchool(@Autowired final DSLContext ctx) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = actor.schools().create(
            new SlInputName(new SlInput(), "Исходная школа")
        );
        MatcherAssert.assertThat(
            "The school director could not revise its school",
            new SlAuthorized(school, actor).revise(
                new SlInputName(new SlInput(), "Новая школа")
            ).uid(),
            Matchers.equalTo(school.uid())
        );
    }

    /**
     * Rejects revisions from a member with only the viewer role.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void deniesRevisionByAViewer(@Autowired final DSLContext ctx) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = new SlsPostgres(ctx).create(
            new SlInputName(new SlInput(), "Школа просмотра")
        );
        new UrsPostgres(ctx, school.uid()).add(actor);
        school.users().roles(actor).grant(Roles.RoleEnum.VIEWER);
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> new SlAuthorized(school, actor).revise(
                new SlInputName(new SlInput(), "Запрещённое имя")
            ),
            "A viewer revised the school"
        );
    }

    /**
     * Checks the current role when a previously allowed school is revised.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void deniesRevisionAfterDirectorRoleIsRevoked(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = actor.schools().create(
            new SlInputName(new SlInput(), "Школа с отзывом")
        );
        final School selected = new SlAuthorized(school, actor);
        school.users().roles(actor).revoke(school.users().roles(actor).list().get(0).uid());
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> selected.revise(new SlInputName(new SlInput(), "Новое имя")),
            "A revoked director retained school modification rights"
        );
    }

    /**
     * Refuses to expose a nested collection after school membership is lost.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void deniesNestedResourcesAfterMembershipIsRemoved(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = actor.schools().create(
            new SlInputName(new SlInput(), "Школа без доступа")
        );
        final School selected = new SlAuthorized(school, actor);
        school.users().remove(actor.uid());
        Assertions.assertThrows(
            AccessDeniedException.class, selected::buildings,
            "A former member accessed the school's buildings"
        );
    }

    /**
     * Checks membership again when a previously obtained users collection is read.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void deniesReadingMembersAfterMembershipIsRemoved(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = actor.schools().create(
            new SlInputName(new SlInput(), "Школа отозванного участника")
        );
        final Users members = new SlAuthorized(school, actor).users();
        school.users().remove(actor.uid());
        Assertions.assertThrows(
            AccessDeniedException.class, members::iterate,
            "A former member read a retained school users collection"
        );
    }

    /**
     * Checks the current role when a retained users collection changes members.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void deniesAddingMembersAfterDirectorRoleIsRevoked(@Autowired final DSLContext ctx)
        throws Exception {
        final Users accounts = new UrsPostgres(ctx);
        final User actor = accounts.register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final User target = accounts.register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = actor.schools().create(
            new SlInputName(new SlInput(), "Школа отозванного директора")
        );
        final Users members = new SlAuthorized(school, actor).users();
        school.users().roles(actor).revoke(school.users().roles(actor).list().getFirst().uid());
        Assertions.assertThrows(
            AccessDeniedException.class, () -> members.add(target),
            "A former director added a school member"
        );
    }

    /**
     * Checks the current role when a retained roles collection grants a role.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void deniesGrantingRolesAfterDirectorRoleIsRevoked(@Autowired final DSLContext ctx)
        throws Exception {
        final Users accounts = new UrsPostgres(ctx);
        final User actor = accounts.register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final User target = accounts.register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = actor.schools().create(
            new SlInputName(new SlInput(), "Школа отозванных ролей")
        );
        school.users().add(target);
        final Roles assigned = new SlAuthorized(school, actor).users().roles(target);
        school.users().roles(actor).revoke(school.users().roles(actor).list().getFirst().uid());
        Assertions.assertThrows(
            AccessDeniedException.class, () -> assigned.grant(Roles.RoleEnum.TEACHER),
            "A former director granted a school role"
        );
    }

    /**
     * Keeps building changes protected through the school decorator.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void deniesBuildingCreationThroughAViewersSchool(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = new SlsPostgres(ctx).create(
            new SlInputName(new SlInput(), "Школа зрителя корпусов")
        );
        school.users().add(actor);
        school.users().roles(actor).grant(Roles.RoleEnum.VIEWER);
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> new SlAuthorized(school, actor).buildings().create(
                new BdInputName(new BdInput(), "Запрещённый корпус")
            ),
            "A viewer created a building through the school"
        );
    }

    /**
     * Configures the isolated database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SlAuthorizedTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", SlAuthorizedTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", SlAuthorizedTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
