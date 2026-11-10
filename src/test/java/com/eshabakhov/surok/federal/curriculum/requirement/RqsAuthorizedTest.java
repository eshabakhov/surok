/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement;

import com.eshabakhov.surok.User;
import com.eshabakhov.surok.federal.curriculum.CrsPostgres;
import com.eshabakhov.surok.federal.curriculum.input.CrInputHttp;
import com.eshabakhov.surok.federal.curriculum.requirement.input.MdRqInput;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInput;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInputHttp;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrsPostgres;
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
 * Verifies that federal requirement objects protect administrator operations.
 * For example, a school member cannot add or revise a plan requirement.
 *
 * @since 0.0.1
 */
@Testcontainers
@SpringBootTest
final class RqsAuthorizedTest {

    /** Isolated database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    /**
     * Rejects requirement creation by a regular user.
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
            () -> new RqsAuthorized(new RqsPostgres(ctx, 91L), actor).create(new RqInput()),
            "A regular user created a federal requirement"
        );
    }

    /**
     * Rejects revision before looking up an unavailable requirement.
     *
     * @param ctx Database context
     * @throws Exception If registration fails
     */
    @Test
    void rejectsRevisionBeforeLookingUpTheRequirement(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> new RqsAuthorized(new RqsPostgres(ctx, 91L), actor).revise(97L, new RqInput()),
            "A missing requirement was looked up before authorization"
        );
    }

    /**
     * Creates a requirement for an administrator.
     *
     * @param ctx Database context
     * @throws Exception If registration or creation fails
     */
    @Test
    void createsARequirementForAnAdministrator(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        actor.roles().grant(Roles.RoleEnum.ADMIN);
        MatcherAssert.assertThat(
            "An administrator could not create a federal requirement",
            new RqsAuthorized(
                new CrsPostgres(ctx).create(
                    new CrInputHttp(
                        Map.of(
                            "title", String.format("Основной план %s", UUID.randomUUID()),
                            "level", "BASIC",
                            "week", "FIVE_DAYS",
                            "version", UUID.randomUUID().toString(),
                            "year", "2026/2027"
                        )
                    )
                ).requirements(), actor
            ).create(
                new RqInputHttp(
                    Map.of("grade", "5", "partType", "OPTIONAL", "hours", "3")
                )
            ).print(new MdRqInput()).print().get("hours"), Matchers.is("3")
        );
    }

    /**
     * Reads a requirement for a regular user.
     *
     * @param ctx Database context
     * @throws Exception If registration or creation fails
     */
    @Test
    void readsARequirementForARegularUser(@Autowired final DSLContext ctx)
        throws Exception {
        final Requirements origin = new CrsPostgres(ctx).create(
            new CrInputHttp(
                Map.of(
                    "title", String.format("Основной план %s", UUID.randomUUID()),
                    "level", "BASIC",
                    "week", "SIX_DAYS",
                    "version", UUID.randomUUID().toString(),
                    "year", "2027/2028"
                )
            )
        ).requirements();
        MatcherAssert.assertThat(
            "A regular user could not read a federal requirement",
            new RqsAuthorized(
                origin,
                new UrsPostgres(ctx).register(
                    UUID.randomUUID().toString(), "ValidPassword1!",
                    String.format("%s@example.com", UUID.randomUUID()), false
                )
            ).requirement(
                origin.create(
                    new RqInputHttp(
                        Map.of("grade", "6", "partType", "OPTIONAL", "hours", "7")
                    )
                ).uid()
            ).print(new MdRqInput()).print().get("hours"),
            Matchers.is("7")
        );
    }

    /**
     * Configures the isolated database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", RqsAuthorizedTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", RqsAuthorizedTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", RqsAuthorizedTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
