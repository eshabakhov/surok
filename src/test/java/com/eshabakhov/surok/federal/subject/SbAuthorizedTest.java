/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.federal.subject.input.SbInput;
import com.eshabakhov.surok.federal.subject.input.SbInputName;
import com.eshabakhov.surok.school.SlsPostgres;
import com.eshabakhov.surok.school.input.SlInput;
import com.eshabakhov.surok.school.input.SlInputName;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrsPostgres;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * A school subject follows the actor's current school role.
 * For example, an administrator reads it without membership and a deputy may rename it.
 *
 * @since 0.0.1
 */
@Testcontainers
@SpringBootTest
final class SbAuthorizedTest {

    /** Isolated database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    /**
     * Allows an administrator to read a subject outside their schools.
     *
     * @param ctx Database context
     * @throws Exception If setup or reading fails
     */
    @Test
    void readsASubjectForAnAdministrator(@Autowired final DSLContext ctx) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        actor.roles().grant(Roles.RoleEnum.ADMIN);
        MatcherAssert.assertThat(
            "An administrator could not read a subject outside their schools",
            new SbAuthorized(
                new SbFake(27L, "Краеведение"),
                new SlsPostgres(ctx).create(
                    new SlInputName(new SlInput(), "Чужая школа администратора")
                ), actor
            ).name(), Matchers.is("Краеведение")
        );
    }

    /**
     * Allows a deputy director to revise a retained school subject.
     *
     * @param ctx Database context
     * @throws Exception If setup or revision fails
     */
    @Test
    void revisesASubjectForADeputyDirector(@Autowired final DSLContext ctx) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = actor.schools().create(
            new SlInputName(new SlInput(), "Школа дополнительных предметов")
        );
        final Subject subject = new SbAuthorized(
            school.subjects().create(new SbInputName(new SbInput(), "Робототехника")),
            school, actor
        );
        school.users().roles(actor).revoke(school.users().roles(actor).list().getFirst().uid());
        school.users().roles(actor).grant(Roles.RoleEnum.DEPUTY_DIRECTOR);
        MatcherAssert.assertThat(
            "A deputy director could not revise a retained school subject",
            subject.revise(new SbInputName(new SbInput(), "Прикладная робототехника")).name(),
            Matchers.is("Прикладная робототехника")
        );
    }

    /**
     * Configures the isolated database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SbAuthorizedTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", SbAuthorizedTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", SbAuthorizedTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
