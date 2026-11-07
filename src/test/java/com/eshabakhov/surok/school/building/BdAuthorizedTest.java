/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.building;

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
 * Verifies permissions on a single protected school building.
 * A cabinet reached through the building remains protected.
 *
 * @since 0.0.1
 */
@Testcontainers
@SpringBootTest
final class BdAuthorizedTest {

    /** Isolated database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    /**
     * Rejects a building revision after its director role is revoked.
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
            new SlInputName(new SlInput(), "Школа корпуса")
        );
        final Building selected = new BdAuthorized(
            school.buildings().create(new BdInputName(new BdInput(), "Корпус")), school, actor
        );
        school.users().roles(actor).revoke(school.users().roles(actor).list().get(0).uid());
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> selected.revise(new BdInputName(new BdInput(), "Другое имя")),
            "A former director revised a building"
        );
    }

    /**
     * Keeps cabinet creation protected when reached through a building.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void deniesCabinetCreationThroughAViewersBuilding(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = new SlsPostgres(ctx).create(
            new SlInputName(new SlInput(), "Школа кабинетов")
        );
        final Building building = school.buildings().create(
            new BdInputName(new BdInput(), "Корпус зрителя")
        );
        school.users().add(actor);
        school.users().roles(actor).grant(Roles.RoleEnum.VIEWER);
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> new BdAuthorized(building, school, actor).cabinets().create(
                new CbInputName(new CbInput(), "Запрещённый кабинет")
            ),
            "A viewer created a cabinet through a building"
        );
    }

    /**
     * Configures the isolated database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", BdAuthorizedTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", BdAuthorizedTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", BdAuthorizedTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
