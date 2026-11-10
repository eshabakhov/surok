/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

import com.eshabakhov.surok.User;
import com.eshabakhov.surok.federal.subject.input.SbInput;
import com.eshabakhov.surok.federal.subject.input.SbInputName;
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
 * Federal subject revisions depend on the actor's current administrator role.
 * For example, a retained federal subject can be revised after granting that role.
 *
 * @since 0.0.1
 */
@Testcontainers
@SpringBootTest
final class SbFederalAuthorizedTest {

    /** Isolated database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    /**
     * Rejects revision through a retained federal subject.
     *
     * @param ctx Database context
     * @throws Exception If registration fails
     */
    @Test
    void rejectsRevisionThroughAProtectedSubject(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> new SbFederalAuthorized(new SbPostgres(ctx, 91L), actor).revise(
                new SbInputName(new SbInput(), "Геометрия")
            ),
            "A regular user revised a retained federal subject"
        );
    }

    /**
     * Revises a retained federal subject after the actor becomes an administrator.
     *
     * @param ctx Database context
     * @throws Exception If registration or revision fails
     */
    @Test
    void revisesARetainedSubjectAfterGrantingTheAdministratorRole(
        @Autowired final DSLContext ctx
    ) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final Subject subject = new SbFederalAuthorized(
            new SbsFederal(ctx).create(
                new SbInputName(new SbInput(), String.format("Физика %s", UUID.randomUUID()))
            ), actor
        );
        actor.roles().grant(Roles.RoleEnum.ADMIN);
        final String name = String.format("Астрономия %s", UUID.randomUUID());
        MatcherAssert.assertThat(
            "A federal subject did not become revisable after granting the administrator role",
            subject.revise(new SbInputName(new SbInput(), name)).name(),
            Matchers.is(name)
        );
    }

    /**
     * Configures the isolated database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", SbFederalAuthorizedTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", SbFederalAuthorizedTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", SbFederalAuthorizedTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
