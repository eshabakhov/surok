/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.teacher;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.school.input.SlInput;
import com.eshabakhov.surok.school.input.SlInputName;
import com.eshabakhov.surok.school.teacher.input.ThInput;
import com.eshabakhov.surok.school.teacher.input.ThInputName;
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
 * Checks current school rights before modifying the teacher collection.
 * For example, a former director cannot add another teacher.
 *
 * @since 0.0.1
 */
@Testcontainers
@SpringBootTest
final class ThsAuthorizedTest {

    /** Isolated database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    /**
     * Denies creation after the director role is revoked.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void deniesCreationAfterDirectorRoleIsRevoked(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = actor.schools().create(
            new SlInputName(new SlInput(), "Школа отозванного директора")
        );
        final Teachers teachers = new ThsAuthorized(school.teachers(), school, actor);
        school.users().roles(actor).revoke(school.users().roles(actor).list().getFirst().uid());
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> teachers.create(new ThInputName(new ThInput(), "Учитель Яна")),
            "A former director created a teacher"
        );
    }

    /**
     * Configures the isolated database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", ThsAuthorizedTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", ThsAuthorizedTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", ThsAuthorizedTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
