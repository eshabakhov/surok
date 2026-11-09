/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.building;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.filter.FiltersRequest;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.page.PageRequest;
import com.eshabakhov.surok.school.SlsPostgres;
import com.eshabakhov.surok.school.building.input.BdInput;
import com.eshabakhov.surok.school.building.input.BdInputName;
import com.eshabakhov.surok.school.input.SlInput;
import com.eshabakhov.surok.school.input.SlInputName;
import com.eshabakhov.surok.sort.SortsRequest;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrsPostgres;
import java.util.List;
import java.util.Set;
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
 * Verifies role checks around a school's building collection.
 * A viewer's global subscription does not authorize building mutations.
 *
 * @since 0.0.1
 */
@Testcontainers
@SpringBootTest
final class BdsAuthorizedTest {

    /** Isolated database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    /**
     * Allows a school director to create a building.
     *
     * @param ctx Database context
     * @throws Exception If setup or creation fails
     */
    @Test
    void createsABuildingForTheDirector(@Autowired final DSLContext ctx) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = actor.schools().create(
            new SlInputName(new SlInput(), "Школа директора")
        );
        MatcherAssert.assertThat(
            "The director could not create a building",
            new BdsAuthorized(school.buildings(), school, actor).create(
                new BdInputName(new BdInput(), "Новый корпус")
            ).print(new MdJson()).print().path("name").asText(),
            Matchers.is("Новый корпус")
        );
    }

    /**
     * Allows a school deputy director to create a building.
     *
     * @param ctx Database context
     * @throws Exception If setup or creation fails
     */
    @Test
    void createsABuildingForTheDeputyDirector(@Autowired final DSLContext ctx) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = new SlsPostgres(ctx).create(
            new SlInputName(new SlInput(), "Школа заместителя")
        );
        school.users().add(actor);
        school.users().roles(actor).grant(Roles.RoleEnum.DEPUTY_DIRECTOR);
        MatcherAssert.assertThat(
            "A deputy director could not create a building",
            new BdsAuthorized(school.buildings(), school, actor).create(
                new BdInputName(new BdInput(), "Корпус заместителя")
            ).uid(), Matchers.greaterThan(0L)
        );
    }

    /**
     * Allows a global administrator to create a building without school membership.
     *
     * @param ctx Database context
     * @throws Exception If setup or creation fails
     */
    @Test
    void createsABuildingForAnAdministrator(@Autowired final DSLContext ctx) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        actor.roles().grant(Roles.RoleEnum.ADMIN);
        final School school = new SlsPostgres(ctx).create(
            new SlInputName(new SlInput(), "Чужая школа администратора")
        );
        MatcherAssert.assertThat(
            "An administrator could not create a building without membership",
            new BdsAuthorized(school.buildings(), school, actor).create(
                new BdInputName(new BdInput(), "Корпус администратора")
            ).uid(), Matchers.greaterThan(0L)
        );
    }

    /**
     * Rejects a viewer even though registration gave it BASIC_MAKER.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void deniesBuildingCreationToAViewer(@Autowired final DSLContext ctx) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = new SlsPostgres(ctx).create(
            new SlInputName(new SlInput(), "Школа зрителя")
        );
        school.users().add(actor);
        school.users().roles(actor).grant(Roles.RoleEnum.VIEWER);
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> new BdsAuthorized(school.buildings(), school, actor).create(
                new BdInputName(new BdInput(), "Запрещённый корпус")
            ),
            "A viewer created a school building"
        );
    }

    /**
     * Checks membership again when an old selection is used.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void deniesSelectedBuildingsAfterMembershipRemoval(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = actor.schools().create(
            new SlInputName(new SlInput(), "Школа с отзывом доступа")
        );
        final Buildings selected = new BdsAuthorized(
            school.buildings(), school, actor
        ).selection(
            new FiltersRequest(Set.of()), new PageRequest(20, 1),
            new SortsRequest(List.of())
        );
        school.users().remove(actor.uid());
        Assertions.assertThrows(
            AccessDeniedException.class, selected::iterate,
            "A former school member iterated its buildings"
        );
    }

    /**
     * Configures the isolated database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", BdsAuthorizedTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", BdsAuthorizedTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", BdsAuthorizedTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
