/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum;

import com.eshabakhov.surok.User;
import com.eshabakhov.surok.federal.curriculum.input.CrInput;
import com.eshabakhov.surok.federal.curriculum.input.CrInputHttp;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrsPostgres;
import java.util.List;
import java.util.Map;
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
     * Creates a federal curriculum for an administrator.
     *
     * @param ctx Database context
     * @throws Exception If registration or creation fails
     */
    @Test
    void createsAFederalCurriculumForAnAdministrator(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        actor.roles().grant(Roles.RoleEnum.ADMIN);
        MatcherAssert.assertThat(
            "An administrator could not create a federal curriculum",
            new CrsAuthorized(new CrsPostgres(ctx), actor).create(
                new CrInputHttp(
                    Map.of(
                        "title", String.format("Начальный план %s", UUID.randomUUID()),
                        "level", "PRIMARY",
                        "week", "FIVE_DAYS",
                        "version", UUID.randomUUID().toString(),
                        "year", "2026/2027"
                    )
                )
            ).grades(), Matchers.is(List.of(1, 2, 3, 4))
        );
    }

    /**
     * Reads a federal curriculum for a regular user.
     *
     * @param ctx Database context
     * @throws Exception If registration or creation fails
     */
    @Test
    void readsAFederalCurriculumForARegularUser(@Autowired final DSLContext ctx)
        throws Exception {
        MatcherAssert.assertThat(
            "A regular user could not read a federal curriculum",
            new CrsAuthorized(
                new CrsPostgres(ctx),
                new UrsPostgres(ctx).register(
                    UUID.randomUUID().toString(), "ValidPassword1!",
                    String.format("%s@example.com", UUID.randomUUID()), false
                )
            ).curriculum(
                new CrsPostgres(ctx).create(
                    new CrInputHttp(
                        Map.of(
                            "title", String.format("Основной план %s", UUID.randomUUID()),
                            "level", "BASIC",
                            "week", "SIX_DAYS",
                            "version", UUID.randomUUID().toString(),
                            "year", "2027/2028"
                        )
                    )
                ).uid()
            ).grades(), Matchers.is(List.of(5, 6, 7, 8, 9))
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
