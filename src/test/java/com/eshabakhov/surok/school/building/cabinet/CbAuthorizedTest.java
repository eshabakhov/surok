/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.building.cabinet;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.school.SlsPostgres;
import com.eshabakhov.surok.school.building.cabinet.input.CbInput;
import com.eshabakhov.surok.school.building.cabinet.input.CbInputName;
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
 * Verifies revision checks on a protected cabinet.
 * A stored cabinet does not keep edit rights after its school role changes.
 *
 * @since 0.0.1
 */
@Testcontainers
@SpringBootTest
final class CbAuthorizedTest {

    /** Isolated database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    /**
     * Rejects cabinet revision when a director role was revoked.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void deniesRevisionAfterDirectorRoleRevocation(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = actor.schools().create(
            new SlInputName(new SlInput(), "Школа кабинета")
        );
        final Cabinet selected = new CbAuthorized(
            school.buildings().create(
                new BdInputName(new BdInput(), "Корпус кабинета")
            ).cabinets().create(new CbInputName(new CbInput(), "Кабинет")),
            school, actor
        );
        school.users().roles(actor).revoke(school.users().roles(actor).list().get(0).uid());
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> selected.revise(new CbInputName(new CbInput(), "Новое имя")),
            "A former director revised a cabinet"
        );
    }

    /**
     * Rejects a viewer's direct attempt to revise a cabinet.
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
            new SlInputName(new SlInput(), "Школа зрителя")
        );
        final Cabinet selected = school.buildings().create(
            new BdInputName(new BdInput(), "Корпус зрителя")
        ).cabinets().create(new CbInputName(new CbInput(), "Кабинет зрителя"));
        school.users().add(actor);
        school.users().roles(actor).grant(Roles.RoleEnum.VIEWER);
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> new CbAuthorized(selected, school, actor).revise(
                new CbInputName(new CbInput(), "Другое имя")
            ),
            "A viewer revised a cabinet"
        );
    }

    /**
     * Allows an administrator to revise a cabinet without school membership.
     *
     * @param ctx Database context
     * @throws Exception If setup or revision fails
     */
    @Test
    void revisesACabinetForAnAdministrator(@Autowired final DSLContext ctx) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        actor.roles().grant(Roles.RoleEnum.ADMIN);
        final School school = new SlsPostgres(ctx).create(
            new SlInputName(new SlInput(), "Чужая школа администратора")
        );
        final Cabinet cabinet = school.buildings().create(
            new BdInputName(new BdInput(), "Корпус администратора")
        ).cabinets().create(new CbInputName(new CbInput(), "Первое имя кабинета"));
        MatcherAssert.assertThat(
            "An administrator could not revise a cabinet without school membership",
            new CbAuthorized(cabinet, school, actor).revise(
                new CbInputName(new CbInput(), "Новое имя кабинета")
            ).uid(), Matchers.equalTo(cabinet.uid())
        );
    }

    /**
     * Configures the isolated database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", CbAuthorizedTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", CbAuthorizedTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", CbAuthorizedTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
