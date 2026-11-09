/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.right;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.User;
import com.eshabakhov.surok.school.SlsPostgres;
import com.eshabakhov.surok.school.input.SlInput;
import com.eshabakhov.surok.school.input.SlInputName;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrsPostgres;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verifies the school-scoped right to change buildings.
 * For example, a deputy director may modify buildings in their own school.
 *
 * @since 0.0.1
 */
@Testcontainers
@SpringBootTest
final class RtDirectorOrDeputyTest {

    /** Isolated database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    /**
     * Allows a deputy director to change buildings in their school.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void allowsADeputyDirectorInTheirSchool(@Autowired final DSLContext ctx) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        final School school = new SlsPostgres(ctx).create(
            new SlInputName(new SlInput(), "Школа заместителя")
        );
        school.users().add(actor);
        school.users().roles(actor).grant(Roles.RoleEnum.DEPUTY_DIRECTOR);
        MatcherAssert.assertThat(
            "A deputy director could not change buildings in their school",
            new RtDirectorOrDeputy(actor, school).allows(), Matchers.is(true)
        );
    }

    /**
     * Denies a director access to another school's buildings.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void deniesADirectorInAnotherSchool(@Autowired final DSLContext ctx) throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        actor.schools().create(new SlInputName(new SlInput(), "Своя школа"));
        MatcherAssert.assertThat(
            "A director could change buildings in another school",
            new RtDirectorOrDeputy(
                actor, new SlsPostgres(ctx).create(
                    new SlInputName(new SlInput(), "Чужая школа")
                )
            ).allows(), Matchers.is(false)
        );
    }

    /**
     * Does not treat a global administrator as a school director.
     *
     * @param ctx Database context
     * @throws Exception If setup fails
     */
    @Test
    void deniesAnAdministratorWithoutASchoolRole(@Autowired final DSLContext ctx)
        throws Exception {
        final User actor = new UrsPostgres(ctx).register(
            UUID.randomUUID().toString(), "ValidPassword1!",
            String.format("%s@example.com", UUID.randomUUID()), false
        );
        actor.roles().grant(Roles.RoleEnum.ADMIN);
        MatcherAssert.assertThat(
            "An administrator was treated as a school director",
            new RtDirectorOrDeputy(
                actor, new SlsPostgres(ctx).create(
                    new SlInputName(new SlInput(), "Школа администратора")
                )
            ).allows(), Matchers.is(false)
        );
    }

    /**
     * Configures the isolated database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", RtDirectorOrDeputyTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", RtDirectorOrDeputyTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", RtDirectorOrDeputyTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
