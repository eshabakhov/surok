/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.autofill;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.federal.curriculum.CrPostgres;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.school.SlAuthorized;
import com.eshabakhov.surok.school.SlsPostgres;
import com.eshabakhov.surok.school.input.SlInput;
import com.eshabakhov.surok.school.input.SlInputName;
import com.eshabakhov.surok.school.schedule.input.SdInput;
import com.eshabakhov.surok.school.schedule.input.SdInputName;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrsPostgres;
import java.util.Map;
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
 * Checks the current school role when a retained draft is applied.
 * For example, revoking a director role blocks a previously obtained draft.
 *
 * @since 0.0.1
 */
@Testcontainers
@SpringBootTest
final class LdAuthorizedTest {

    /** Isolated database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    /**
     * Denies application after the director role is revoked.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void deniesApplicationAfterDirectorRoleIsRevoked(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = actor.schools().create(
            new SlInputName(new SlInput(), "Школа черновиков")
        );
        final LoadDraft draft = new SlAuthorized(school, actor).schedules().create(
            new SdInputName(new SdInput(), "Учебный год")
        ).draft();
        school.users().roles(actor).revoke(school.users().roles(actor).list().getFirst().uid());
        Assertions.assertThrows(
            AccessDeniedException.class,
            draft::apply,
            "A former director applied a retained load draft"
        );
    }

    /**
     * Denies reading after school membership is removed.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void deniesReadingAfterMembershipIsRemoved(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = actor.schools().create(
            new SlInputName(new SlInput(), "Школа закрытых черновиков")
        );
        final LoadDraft draft = new SlAuthorized(school, actor).schedules().create(
            new SdInputName(new SdInput(), "Учебный год")
        ).draft();
        school.users().remove(actor.uid());
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> draft.print(new MdJson()),
            "A former school member read a retained load draft"
        );
    }

    /**
     * Denies saving a retained draft after the director becomes a viewer.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void deniesSavingAfterDirectorBecomesViewer(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = actor.schools().create(
            new SlInputName(new SlInput(), "Школа черновиков для просмотра")
        );
        final LoadDraft draft = new SlAuthorized(school, actor).schedules().create(
            new SdInputName(new SdInput(), "Учебный год")
        ).draft();
        school.users().roles(actor).revoke(school.users().roles(actor).list().getFirst().uid());
        school.users().roles(actor).grant(Roles.RoleEnum.VIEWER);
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> draft.save(new CrPostgres(ctx, 61L), Map.of()),
            "A viewer saved a retained load draft"
        );
    }

    /**
     * Lets an administrator reach a draft without membership in its school.
     *
     * @param ctx Database context
     * @throws Exception If setup or reading fails
     */
    @Test
    void readsAnAbsentDraftForAnAdministrator(@Autowired final DSLContext ctx) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        actor.roles().grant(Roles.RoleEnum.ADMIN);
        final School school = new SlsPostgres(ctx).create(
            new SlInputName(new SlInput(), "Чужая школа администратора")
        );
        Assertions.assertThrows(
            LoadDraftNotFoundException.class,
            () -> new LdAuthorized(
                school.schedules().create(
                    new SdInputName(new SdInput(), "Учебный год без черновика")
                ).draft(), school, actor
            ).curriculum(),
            "An administrator could not inspect a draft outside their schools"
        );
    }

    /**
     * Configures the isolated database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", LdAuthorizedTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", LdAuthorizedTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", LdAuthorizedTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
