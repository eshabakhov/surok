/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.school.SlsPostgres;
import com.eshabakhov.surok.school.input.SlInput;
import com.eshabakhov.surok.school.input.SlInputName;
import com.eshabakhov.surok.school.schedule.input.SdInput;
import com.eshabakhov.surok.school.schedule.input.SdInputName;
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
 * Checks a director's current school role before revising a schedule.
 * For example, a retained schedule cannot be revised after role revocation.
 *
 * @since 0.0.1
 */
@Testcontainers
@SpringBootTest
final class SdAuthorizedTest {

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
            new SlInputName(new SlInput(), "Школа расписаний")
        );
        final Schedule schedule = new SdAuthorized(
            school.schedules().create(new SdInputName(new SdInput(), "Учебный год")),
            school, actor
        );
        school.users().roles(actor).revoke(school.users().roles(actor).list().getFirst().uid());
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> schedule.revise(new SdInputName(new SdInput(), "Новый учебный год")),
            "A former director revised a retained schedule"
        );
    }

    /**
     * Allows an administrator to revise a schedule without school membership.
     *
     * @param ctx Database context
     * @throws Exception If setup or revision fails
     */
    @Test
    void revisesAScheduleForAnAdministrator(@Autowired final DSLContext ctx) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        actor.roles().grant(Roles.RoleEnum.ADMIN);
        final School school = new SlsPostgres(ctx).create(
            new SlInputName(new SlInput(), "Чужая школа администратора")
        );
        final Schedule schedule = school.schedules().create(
            new SdInputName(new SdInput(), "Исходный год")
        );
        MatcherAssert.assertThat(
            "An administrator could not revise a schedule without school membership",
            new SdAuthorized(schedule, school, actor).revise(
                new SdInputName(new SdInput(), "Новый учебный год")
            ).uid(), Matchers.equalTo(schedule.uid())
        );
    }

    /**
     * Denies access to teaching loads after school membership is removed.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void deniesTeachingLoadsAfterMembershipRemoval(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = actor.schools().create(
            new SlInputName(new SlInput(), "Школа без нагрузки")
        );
        final Schedule selected = new SdAuthorized(
            school.schedules().create(new SdInputName(new SdInput(), "Учебный год")),
            school, actor
        );
        school.users().remove(actor.uid());
        Assertions.assertThrows(
            AccessDeniedException.class, selected::loads,
            "A former school member accessed the schedule loads"
        );
    }

    /**
     * Denies access to a load draft after school membership is removed.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void deniesTheLoadDraftAfterMembershipRemoval(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = actor.schools().create(
            new SlInputName(new SlInput(), "Школа без черновика")
        );
        final Schedule selected = new SdAuthorized(
            school.schedules().create(new SdInputName(new SdInput(), "Учебный год")),
            school, actor
        );
        school.users().remove(actor.uid());
        Assertions.assertThrows(
            AccessDeniedException.class, selected::draft,
            "A former school member accessed the schedule load draft"
        );
    }

    /**
     * Configures the isolated database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SdAuthorizedTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", SdAuthorizedTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", SdAuthorizedTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
