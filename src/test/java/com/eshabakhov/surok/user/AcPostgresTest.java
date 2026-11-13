/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import java.util.Map;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * PostgreSQL account details and the credentials used by Spring Security.
 * For example, corporate and personal accounts retain their stored identity and password hash.
 *
 * @since 0.0.1
 */
@Testcontainers
@DirtiesContext
@SpringBootTest
final class AcPostgresTest {

    /** Isolated account database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine");

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void preservesAccountIdentityAndAvailability(
        final boolean corporate, @Autowired final DSLContext ctx
    ) throws Exception {
        final String name = String.format("Пользователь-%s", UUID.randomUUID());
        final Account account = new AcPostgres(
            ctx,
            new UrsPostgres(ctx).register(
                name, "ValidPassword1!",
                String.format("%s@example.com", UUID.randomUUID()), corporate
            ).uid()
        );
        MatcherAssert.assertThat(
            "The account lost its stored identity, type or availability",
            Map.of(
                "username", account.username(), "corporate", account.corporate(),
                "alive", account.alive(), "authenticated", account.authenticated()
            ),
            Matchers.is(
                Map.of(
                    "username", name, "corporate", corporate, "alive", true, "authenticated", true
                )
            )
        );
    }

    @Test
    void suppliesTheStoredPasswordHashToAuthentication(@Autowired final DSLContext ctx)
        throws Exception {
        final String password = String.format("Пароль-%s!", UUID.randomUUID());
        MatcherAssert.assertThat(
            "The account did not supply the stored hash for password authentication",
            new BCryptPasswordEncoder().matches(
                password,
                new AcPostgres(
                    ctx,
                    new UrsPostgres(ctx).register(
                        String.format("Учитель-%s", UUID.randomUUID()), password,
                        String.format("%s@example.com", UUID.randomUUID()), false
                    ).uid()
                ).password()
            ),
            Matchers.is(true)
        );
    }

    /**
     * Apply the application schema to the isolated account database.
     *
     * @param registry Spring properties
     */
    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", AcPostgresTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", AcPostgresTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", AcPostgresTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
