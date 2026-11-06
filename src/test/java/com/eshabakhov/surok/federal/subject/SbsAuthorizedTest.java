/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.federal.subject.input.SbInput;
import com.eshabakhov.surok.federal.subject.input.SbInputName;
import com.eshabakhov.surok.school.input.SlInput;
import com.eshabakhov.surok.school.input.SlInputName;
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
 * School subjects follow the actor's current role in their owning school.
 * For example, a retained collection rejects a former director with a global tariff.
 *
 * @since 0.0.1
 */
@Testcontainers
@SpringBootTest
final class SbsAuthorizedTest {

    /** Isolated database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    /**
     * Rejects a tariff holder after the school director role is revoked.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void rejectsCreationAfterTheDirectorRoleIsRevoked(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = actor.schools().create(
            new SlInputName(new SlInput(), "Школа предметов")
        );
        final Subjects subjects = new SbsAuthorized(school.subjects(), school, actor);
        school.users().roles(actor).revoke(school.users().roles(actor).list().getFirst().uid());
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> subjects.create(new SbInputName(new SbInput(), "Робототехника")),
            "A tariff role alone allowed creating a school subject"
        );
    }

    /**
     * Rejects revision before looking up an unavailable school subject.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void rejectsRevisionBeforeLookingUpTheSubject(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = actor.schools().create(
            new SlInputName(new SlInput(), "Школа геометрии")
        );
        school.users().roles(actor).revoke(school.users().roles(actor).list().getFirst().uid());
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> new SbsAuthorized(school.subjects(), school, actor).revise(
                91L, new SbInputName(new SbInput(), "Геометрия")
            ),
            "A missing school subject was looked up before authorization"
        );
    }

    /**
     * Rejects revision through a retained school subject after role revocation.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void rejectsRevisionThroughAProtectedSubject(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = actor.schools().create(
            new SlInputName(new SlInput(), "Школа истории")
        );
        final Subject subject = new SbsAuthorized(
            school.subjects(), school, actor
        ).create(new SbInputName(new SbInput(), "История края"));
        school.users().roles(actor).revoke(school.users().roles(actor).list().getFirst().uid());
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> subject.revise(new SbInputName(new SbInput(), "Местная история")),
            "A former director revised a retained school subject"
        );
    }

    /**
     * Configures the isolated database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SbsAuthorizedTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", SbsAuthorizedTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", SbsAuthorizedTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
