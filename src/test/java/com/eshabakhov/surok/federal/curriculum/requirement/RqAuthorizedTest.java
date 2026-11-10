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
 * Federal requirement revisions depend on the actor's current administrator role.
 * For example, a retained requirement becomes revisable after granting that role.
 *
 * @since 0.0.1
 */
@Testcontainers
@SpringBootTest
final class RqAuthorizedTest {

    /** Isolated database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    /**
     * Rejects revision through a retained requirement entity.
     *
     * @param ctx Database context
     * @throws Exception If registration fails
     */
    @Test
    void rejectsRevisionThroughAProtectedRequirement(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> new RqAuthorized(new RqMandatory(ctx, 91L), actor).revise(new RqInput()),
            "A regular user revised a federal requirement"
        );
    }

    /**
     * Revises a retained requirement after granting the administrator role.
     *
     * @param ctx Database context
     * @throws Exception If registration or revision fails
     */
    @Test
    void revisesARetainedRequirementAfterGrantingTheAdministratorRole(
        @Autowired final DSLContext ctx
    ) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final Requirement requirement = new RqAuthorized(
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
            ).requirements().create(
                new RqInputHttp(
                    Map.of("grade", "3", "partType", "OPTIONAL", "hours", "2")
                )
            ), actor
        );
        actor.roles().grant(Roles.RoleEnum.ADMIN);
        MatcherAssert.assertThat(
            "A retained requirement did not become revisable after granting administrator role",
            requirement.revise(
                new RqInputHttp(
                    Map.of("grade", "3", "partType", "OPTIONAL", "hours", "5")
                )
            ).print(new MdRqInput()).print().get("hours"), Matchers.is("5")
        );
    }

    /**
     * Configures the isolated database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", RqAuthorizedTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", RqAuthorizedTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", RqAuthorizedTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
