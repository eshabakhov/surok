/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.building.cabinet;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.school.SlsPostgres;
import com.eshabakhov.surok.school.building.Building;
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
 * Verifies school roles around a building's cabinet collection.
 * A deputy director can create, while a viewer cannot remove cabinets.
 *
 * @since 0.0.1
 */
@Testcontainers
@SpringBootTest
final class CbsAuthorizedTest {

    /** Isolated database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    /**
     * Allows a deputy director to create a cabinet.
     *
     * @param ctx Database context
     * @throws Exception If setup or creation fails
     */
    @Test
    void createsACabinetForADeputyDirector(@Autowired final DSLContext ctx) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = new SlsPostgres(ctx).create(
            new SlInputName(new SlInput(), "Школа заместителя")
        );
        final Building building = school.buildings().create(
            new BdInputName(new BdInput(), "Главный корпус")
        );
        school.users().add(actor);
        school.users().roles(actor).grant(Roles.RoleEnum.DEPUTY_DIRECTOR);
        MatcherAssert.assertThat(
            "A deputy director could not create a cabinet",
            new CbsAuthorized(building.cabinets(), school, actor).create(
                new CbInputName(new CbInput(), "Кабинет химии")
            ).print(new MdJson()).print().path("name").asText(),
            Matchers.is("Кабинет химии")
        );
    }

    /**
     * Rejects removal by a school viewer with a global subscription.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void deniesCabinetRemovalToAViewer(@Autowired final DSLContext ctx) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = new SlsPostgres(ctx).create(
            new SlInputName(new SlInput(), "Школа зрителя")
        );
        final Building building = school.buildings().create(
            new BdInputName(new BdInput(), "Корпус зрителя")
        );
        final long id = building.cabinets().create(
            new CbInputName(new CbInput(), "Кабинет зрителя")
        ).uid();
        school.users().add(actor);
        school.users().roles(actor).grant(Roles.RoleEnum.VIEWER);
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> new CbsAuthorized(building.cabinets(), school, actor).remove(id),
            "A school viewer removed a cabinet"
        );
    }

    /**
     * Configures the isolated database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", CbsAuthorizedTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", CbsAuthorizedTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", CbsAuthorizedTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
