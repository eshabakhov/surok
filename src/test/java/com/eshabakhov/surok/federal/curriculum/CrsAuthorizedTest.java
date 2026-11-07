/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum;

import com.eshabakhov.surok.User;
import com.eshabakhov.surok.federal.curriculum.input.CrInput;
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
 * Verifies that federal curriculum collections protect administrator operations.
 * For example, a school member cannot create a federal curriculum.
 *
 * @since 0.0.1
 */
@Testcontainers
@SpringBootTest
final class CrsAuthorizedTest {

    /** Isolated database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    /**
     * Rejects federal curriculum creation by a regular user.
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
            () -> new CrsAuthorized(new CrsPostgres(ctx), actor).create(new CrInput()),
            "A regular user created a federal curriculum"
        );
    }

    /**
     * Rejects revision through a retained federal curriculum entity.
     *
     * @param ctx Database context
     * @throws Exception If registration fails
     */
    @Test
    void rejectsRevisionThroughAProtectedCurriculum(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> new CrAuthorized(new CrPostgres(ctx, 91L), actor).revise(new CrInput()),
            "A regular user revised a federal curriculum"
        );
    }

    /**
     * Rejects revision before looking up an unavailable curriculum.
     *
     * @param ctx Database context
     * @throws Exception If registration fails
     */
    @Test
    void rejectsRevisionBeforeLookingUpTheCurriculum(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> new CrsAuthorized(new CrsPostgres(ctx), actor).revise(91L, new CrInput()),
            "A missing curriculum was looked up before authorization"
        );
    }

    /**
     * Configures the isolated database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", CrsAuthorizedTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", CrsAuthorizedTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", CrsAuthorizedTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
