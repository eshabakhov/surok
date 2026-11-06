/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.federal.subject.SbFake;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.school.input.SlInput;
import com.eshabakhov.surok.school.input.SlInputName;
import com.eshabakhov.surok.school.schedule.load.hours.WhFixed;
import com.eshabakhov.surok.school.schoolclass.ScFake;
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
 * Checks school membership before printing a retained teaching load.
 * For example, a former member cannot inspect its hours.
 *
 * @since 0.0.1
 */
@Testcontainers
@SpringBootTest
final class ClAuthorizedTest {

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
        final ClassLoad load = new ClAuthorized(
            new ClFake(31L, new ScFake(17L), new SbFake(23L), new WhFixed(5)),
            school, actor
        );
        school.users().remove(actor.uid());
        Assertions.assertThrows(
            AccessDeniedException.class,
            () -> load.print(new MdJson()),
            "A former school member printed a retained teaching load"
        );
    }

    /**
     * Configures the isolated database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", ClAuthorizedTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", ClAuthorizedTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", ClAuthorizedTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
