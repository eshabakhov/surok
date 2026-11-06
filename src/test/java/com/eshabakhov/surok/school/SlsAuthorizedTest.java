/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school;

import com.eshabakhov.surok.Schools;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.filter.FiltersRequest;
import com.eshabakhov.surok.page.PageRequest;
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
 * Checks school collection permissions against live memberships.
 * For example, one member cannot select another school's details.
 *
 * @since 0.0.1
 */
@Testcontainers
@SpringBootTest
final class SlsAuthorizedTest {

    /** Isolated database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    /**
     * Hides schools outside the acting user's memberships.
     *
     * @param ctx Database context
     * @throws Exception If creation or selection fails
     */
    @Test
    void selectsOnlySchoolsOfTheActingUser(@Autowired final DSLContext ctx) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        actor.schools().create(new SlInputName(new SlInput(), "Школа участника"));
        new SlsPostgres(ctx).create(new SlInputName(new SlInput(), "Чужая школа"));
        MatcherAssert.assertThat(
            "School selection exposed an unrelated school",
            new SlsAuthorized(actor.schools(), actor).selection(
                new FiltersRequest(Set.of()), new PageRequest(20, 1),
                new SortsRequest(List.of())
            ).iterate(),
            Matchers.iterableWithSize(1)
        );
    }

    /**
     * Appoints the creator as a director through the protected collection.
     *
     * @param ctx Database context
     * @throws Exception If registration or creation fails
     */
    @Test
    void appointsTheCreatorAsDirector(@Autowired final DSLContext ctx) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        MatcherAssert.assertThat(
            "Protected creation did not assign the school director",
            new SlsAuthorized(actor.schools(), actor).create(
                new SlInputName(new SlInput(), "Новая школа")
            ).users().roles(actor).contains(Roles.RoleEnum.DIRECTOR),
            Matchers.is(true)
        );
    }

    /**
     * Rejects removal by a non-administrator even when the school is visible.
     *
     * @param ctx Database context
     * @throws Exception If registration or creation fails
     */
    @Test
    void deniesRemovalByASchoolDirector(@Autowired final DSLContext ctx) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final long id = actor.schools().create(
            new SlInputName(new SlInput(), "Школа директора")
        ).uid();
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> new SlsAuthorized(actor.schools(), actor).remove(id),
            "A school director removed a school"
        );
    }

    /**
     * Rejects direct lookup of a school outside the user's memberships.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void deniesDirectLookupOfAnotherSchool(@Autowired final DSLContext ctx) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final long id = new SlsPostgres(ctx).create(
            new SlInputName(new SlInput(), "Чужая гимназия")
        ).uid();
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> new SlsAuthorized(actor.schools(), actor).school(id),
            "A school member accessed an unrelated school"
        );
    }

    /**
     * Allows an administrator to remove a school outside its memberships.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void allowsAnAdministratorToRemoveASchool(@Autowired final DSLContext ctx) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        actor.roles().grant(Roles.RoleEnum.ADMIN);
        final long id = new SlsPostgres(ctx).create(
            new SlInputName(new SlInput(), "Школа администратора")
        ).uid();
        new SlsAuthorized(new SlsPostgres(ctx), actor).remove(id);
        MatcherAssert.assertThat(
            "An administrator could not remove a school",
            new SlsPostgres(ctx).contains(id), Matchers.is(false)
        );
    }

    /**
     * Uses the current membership scope if administrator rights are revoked.
     *
     * @param ctx Database context
     * @throws Exception If setup or selection fails
     */
    @Test
    void narrowsSelectionAfterAdministratorRoleIsRevoked(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        actor.schools().create(new SlInputName(new SlInput(), "Своя школа"));
        new SlsPostgres(ctx).create(new SlInputName(new SlInput(), "Чужая школа"));
        final long role = actor.roles().grant(Roles.RoleEnum.ADMIN).uid();
        final Schools selected = new SlsAuthorized(new SlsPostgres(ctx), actor);
        actor.roles().revoke(role);
        MatcherAssert.assertThat(
            "Revoked administrator selected unrelated schools",
            selected.selection(
                new FiltersRequest(Set.of()), new PageRequest(20, 1),
                new SortsRequest(List.of())
            ).iterate(),
            Matchers.iterableWithSize(1)
        );
    }

    /**
     * Configures the isolated database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SlsAuthorizedTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", SlsAuthorizedTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", SlsAuthorizedTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
