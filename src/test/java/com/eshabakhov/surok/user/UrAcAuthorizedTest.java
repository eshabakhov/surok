/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.school.SlsPostgres;
import com.eshabakhov.surok.school.building.input.BdInput;
import com.eshabakhov.surok.school.building.input.BdInputName;
import com.eshabakhov.surok.school.input.SlInput;
import com.eshabakhov.surok.school.input.SlInputName;
import com.eshabakhov.surok.security.OaYaml;
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
 * Protected school navigation from a user with an account.
 * For example, a director in one school remains a student in another school.
 *
 * @since 0.0.1
 */
@Testcontainers
@SpringBootTest
final class UrAcAuthorizedTest {

    /** Isolated database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void createsASchoolForAnAccountWithoutRoles(@Autowired final DSLContext ctx) throws Exception {
        final UrAccount actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        for (final Role role : actor.roles().list()) {
            actor.roles().revoke(role.uid());
        }
        MatcherAssert.assertThat(
            "An account without roles did not become director of its new school",
            new UrAcAuthorized(actor, new OaYaml().operations()).schools().create(
                new SlInputName(new SlInput(), "Новая школа")
            ).users().roles(actor).contains(Roles.RoleEnum.DIRECTOR), Matchers.is(true)
        );
    }

    @Test
    void keepsTheDirectorRoleWithinItsSchool(@Autowired final DSLContext ctx) throws Exception {
        final UrAccount actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        actor.schools().create(new SlInputName(new SlInput(), "Школа директора"));
        final School school = new SlsPostgres(ctx).create(
            new SlInputName(new SlInput(), "Школа ученика")
        );
        school.users().add(actor);
        school.users().roles(actor).grant(Roles.RoleEnum.STUDENT);
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> new UrAcAuthorized(
                actor, new OaYaml().operations()
            ).schools().school(school.uid())
                .buildings().create(new BdInputName(new BdInput(), "Закрытый корпус")),
            "A director changed a school where their role was student"
        );
    }

    @Test
    void rejectsAFormerDirectorUsingARetainedSchool(@Autowired final DSLContext ctx)
        throws Exception {
        final UrAccount actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = new UrAcAuthorized(
            actor, new OaYaml().operations()
        ).schools().create(
            new SlInputName(new SlInput(), "Школа с изменяемыми правами")
        );
        for (final Role role : actor.schools().school(school.uid()).users().roles(actor).list()) {
            actor.schools().school(school.uid()).users().roles(actor).revoke(role.uid());
        }
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> school.buildings().create(new BdInputName(new BdInput(), "Недоступный корпус")),
            "A former director changed a retained school after revocation"
        );
    }

    /**
     * Configure the isolated database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", UrAcAuthorizedTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", UrAcAuthorizedTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", UrAcAuthorizedTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
