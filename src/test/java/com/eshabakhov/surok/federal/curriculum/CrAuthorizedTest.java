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
 * Federal curriculum revision checks the actor's current administrator role.
 * For example, a retained curriculum becomes revisable after the role is granted.
 *
 * @since 0.0.1
 */
@Testcontainers
@SpringBootTest
final class CrAuthorizedTest {

    /** Isolated database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

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
     * Revises a retained curriculum after granting the administrator role.
     *
     * @param ctx Database context
     * @throws Exception If registration or revision fails
     */
    @Test
    void revisesARetainedCurriculumAfterGrantingTheAdministratorRole(
        @Autowired final DSLContext ctx
    ) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final Curriculum curriculum = new CrAuthorized(
            new CrsPostgres(ctx).create(
                new CrInputHttp(
                    Map.of(
                        "title", String.format("Начальный план %s", UUID.randomUUID()),
                        "level", "PRIMARY",
                        "week", "FIVE_DAYS",
                        "version", UUID.randomUUID().toString(),
                        "year", "2026/2027"
                    )
                )
            ), actor
        );
        actor.roles().grant(Roles.RoleEnum.ADMIN);
        MatcherAssert.assertThat(
            "A retained curriculum did not become revisable after granting administrator role",
            curriculum.revise(
                new CrInputHttp(
                    Map.of(
                        "title", String.format("Средний план %s", UUID.randomUUID()),
                        "level", "SECONDARY",
                        "week", "SIX_DAYS",
                        "version", UUID.randomUUID().toString(),
                        "year", "2028/2029"
                    )
                )
            ).grades(), Matchers.is(List.of(10, 11))
        );
    }

    /**
     * Configures the isolated database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", CrAuthorizedTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", CrAuthorizedTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", CrAuthorizedTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
