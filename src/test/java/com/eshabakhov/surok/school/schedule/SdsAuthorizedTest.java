/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.filter.FiltersRequest;
import com.eshabakhov.surok.page.PageRequest;
import com.eshabakhov.surok.school.SlsPostgres;
import com.eshabakhov.surok.school.input.SlInput;
import com.eshabakhov.surok.school.input.SlInputName;
import com.eshabakhov.surok.school.schedule.input.SdInput;
import com.eshabakhov.surok.school.schedule.input.SdInputName;
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
 * Checks a director's current school role before creating a schedule.
 * For example, a retained collection cannot create after role revocation.
 *
 * @since 0.0.1
 */
@Testcontainers
@SpringBootTest
final class SdsAuthorizedTest {

    /** Isolated database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    /**
     * Denies creation after the director role is revoked.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void deniesCreationAfterDirectorRoleIsRevoked(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = actor.schools().create(
            new SlInputName(new SlInput(), "Школа расписаний")
        );
        final Schedules schedules = new SdsAuthorized(school.schedules(), school, actor);
        school.users().roles(actor).revoke(school.users().roles(actor).list().getFirst().uid());
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> schedules.create(new SdInputName(new SdInput(), "Учебный год")),
            "A former director created a schedule"
        );
    }

    /**
     * Allows an administrator to create a schedule without school membership.
     *
     * @param ctx Database context
     * @throws Exception If setup or creation fails
     */
    @Test
    void createsAScheduleForAnAdministrator(@Autowired final DSLContext ctx) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        actor.roles().grant(Roles.RoleEnum.ADMIN);
        final School school = new SlsPostgres(ctx).create(
            new SlInputName(new SlInput(), "Чужая школа администратора")
        );
        MatcherAssert.assertThat(
            "An administrator could not create a schedule without school membership",
            new SdsAuthorized(school.schedules(), school, actor).create(
                new SdInputName(new SdInput(), "Учебный год администратора")
            ).uid(), Matchers.greaterThan(0L)
        );
    }

    /**
     * Rejects access to a schedule in another school.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void deniesScheduleReadingToAnOutsider(@Autowired final DSLContext ctx) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        actor.schools().create(new SlInputName(new SlInput(), "Своя школа"));
        final School school = new SlsPostgres(ctx).create(
            new SlInputName(new SlInput(), "Чужая школа")
        );
        final long id = school.schedules().create(
            new SdInputName(new SdInput(), "Чужой учебный год")
        ).uid();
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> new SdsAuthorized(school.schedules(), school, actor).schedule(id),
            "An outsider read a schedule in another school"
        );
    }

    /**
     * Checks membership again when a retained selection is iterated.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void deniesSelectedSchedulesAfterMembershipRemoval(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = actor.schools().create(
            new SlInputName(new SlInput(), "Школа с отзывом доступа")
        );
        final Schedules selected = new SdsAuthorized(
            school.schedules(), school, actor
        ).selection(
            new FiltersRequest(Set.of()), new PageRequest(20, 1),
            new SortsRequest(List.of())
        );
        school.users().remove(actor.uid());
        Assertions.assertThrows(
            AccessDeniedException.class, selected::iterate,
            "A former school member iterated its schedules"
        );
    }

    /**
     * Configures the isolated database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SdsAuthorizedTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", SdsAuthorizedTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", SdsAuthorizedTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
