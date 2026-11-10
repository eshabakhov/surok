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
 * Federal subjects can be changed only by the global administrator.
 * For example, a registered school user cannot create a shared subject.
 *
 * @since 0.0.1
 */
@Testcontainers
@SpringBootTest
final class SbsFederalAuthorizedTest {

    /** Isolated database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    /**
     * Rejects federal subject creation by a regular user.
     *
     * @param ctx Database context
     * @throws Exception If registration fails
     */
    @Test
    void rejectsCreationByARegularUser(@Autowired final DSLContext ctx) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> new SbsFederalAuthorized(new SbsFederal(ctx), actor).create(
                new SbInputName(new SbInput(), "Геометрия")
            ),
            "A regular user created a federal subject"
        );
    }

    /**
     * Rejects revision before looking up an unavailable federal subject.
     *
     * @param ctx Database context
     * @throws Exception If registration fails
     */
    @Test
    void rejectsRevisionBeforeLookingUpTheSubject(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> new SbsFederalAuthorized(new SbsFederal(ctx), actor).revise(
                97L, new SbInputName(new SbInput(), "Геометрия")
            ),
            "A missing federal subject was looked up before authorization"
        );
    }

    /**
     * Allows an administrator to create a federal subject.
     *
     * @param ctx Database context
     * @throws Exception If registration or creation fails
     */
    @Test
    void createsAFederalSubjectForAnAdministrator(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        actor.roles().grant(Roles.RoleEnum.ADMIN);
        MatcherAssert.assertThat(
            "An administrator could not create a federal subject",
            new SbsFederalAuthorized(new SbsFederal(ctx), actor).create(
                new SbInputName(new SbInput(), String.format("Геометрия %s", UUID.randomUUID()))
            ).origin(),
            Matchers.is(Subject.Origin.FEDERAL)
        );
    }

    /**
     * Reads a federal subject without an administrator role.
     *
     * @param ctx Database context
     * @throws Exception If registration or creation fails
     */
    @Test
    void readsAFederalSubjectForARegularUser(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final String name = String.format("Астрономия %s", UUID.randomUUID());
        MatcherAssert.assertThat(
            "A regular user could not read a federal subject",
            new SbsFederalAuthorized(new SbsFederal(ctx), actor).subject(
                new SbsFederal(ctx).create(new SbInputName(new SbInput(), name)).uid()
            ).name(),
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
        registry.add("spring.datasource.url", SbsFederalAuthorizedTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", SbsFederalAuthorizedTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", SbsFederalAuthorizedTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
