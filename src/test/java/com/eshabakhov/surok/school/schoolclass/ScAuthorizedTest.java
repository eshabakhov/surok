/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schoolclass;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.school.SlsPostgres;
import com.eshabakhov.surok.school.input.SlInput;
import com.eshabakhov.surok.school.input.SlInputName;
import com.eshabakhov.surok.school.schoolclass.input.ScInput;
import com.eshabakhov.surok.school.schoolclass.input.ScInputGrade;
import com.eshabakhov.surok.school.schoolclass.input.ScInputLitera;
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
 * Checks current school rights before revising one class.
 * For example, a retained class cannot be renamed by a former director.
 *
 * @since 0.0.1
 */
@Testcontainers
@SpringBootTest
final class ScAuthorizedTest {

    /** Isolated database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    /**
     * Denies revision after the director role is revoked.
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
            new SlInputName(new SlInput(), "Школа класса")
        );
        final SchoolClass clazz = new ScAuthorized(
            school.schoolClasses().create(
                new ScInputLitera(new ScInputGrade(new ScInput(), "7"), "Я")
            ), school, actor
        );
        school.users().roles(actor).revoke(school.users().roles(actor).list().getFirst().uid());
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> clazz.revise(
                new ScInputLitera(new ScInputGrade(new ScInput(), "8"), "Я")
            ),
            "A former director revised a retained school class"
        );
    }

    /**
     * Allows a school viewer to read a class grade.
     *
     * @param ctx Database context
     * @throws Exception If setup or reading fails
     */
    @Test
    void readsTheGradeForAViewer(@Autowired final DSLContext ctx) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = new SlsPostgres(ctx).create(
            new SlInputName(new SlInput(), "Школа зрителя")
        );
        school.users().add(actor);
        school.users().roles(actor).grant(Roles.RoleEnum.VIEWER);
        MatcherAssert.assertThat(
            "A school viewer could not read a class grade",
            new ScAuthorized(
                school.schoolClasses().create(
                    new ScInputLitera(new ScInputGrade(new ScInput(), "7"), "Я")
                ), school, actor
            ).grade(), Matchers.is(7)
        );
    }

    /**
     * Allows an administrator to revise a class without school membership.
     *
     * @param ctx Database context
     * @throws Exception If setup or revision fails
     */
    @Test
    void revisesAClassForAnAdministrator(@Autowired final DSLContext ctx) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        actor.roles().grant(Roles.RoleEnum.ADMIN);
        final School school = new SlsPostgres(ctx).create(
            new SlInputName(new SlInput(), "Чужая школа администратора")
        );
        final SchoolClass clazz = school.schoolClasses().create(
            new ScInputLitera(new ScInputGrade(new ScInput(), "7"), "Я")
        );
        MatcherAssert.assertThat(
            "An administrator could not revise a class without school membership",
            new ScAuthorized(clazz, school, actor).revise(
                new ScInputLitera(new ScInputGrade(new ScInput(), "8"), "Я")
            ).uid(), Matchers.equalTo(clazz.uid())
        );
    }

    /**
     * Configures the isolated database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", ScAuthorizedTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", ScAuthorizedTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", ScAuthorizedTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
