/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.media.MdJson;
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
 * Checks school membership before printing applied teaching loads.
 * For example, a retained collection cannot print after membership removal.
 *
 * @since 0.0.1
 */
@Testcontainers
@SpringBootTest
final class ClsAuthorizedTest {

    /** Isolated database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    /**
     * Denies printing after school membership is removed.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void deniesPrintingAfterMembershipIsRemoved(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = actor.schools().create(
            new SlInputName(new SlInput(), "Школа учебной нагрузки")
        );
        final ClassLoads loads = new ClsAuthorized(
            new ClassLoadsPostgres(ctx, 41L), school, actor
        );
        school.users().remove(actor.uid());
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> loads.print(new MdJson()),
            "A former school member printed retained teaching loads"
        );
    }

    /**
     * Configures the isolated database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", ClsAuthorizedTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", ClsAuthorizedTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", ClsAuthorizedTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
