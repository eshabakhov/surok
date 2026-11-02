/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school;

import com.eshabakhov.surok.media.MdJson;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Versioned school representations expose the school identity and name.
 *
 * @since 0.0.1
 * @checkstyle ProhibitFieldsInTestClassesCheck (1000 lines)
 * @checkstyle NoJavadocForPrivateMethodsCheck (1000 lines)
 */
@Testcontainers
@DirtiesContext
@SpringBootTest
@SuppressWarnings({"PMD.UnusedPrivateMethod", "UnusedMethod"})
final class SlV1Test {

    /** Disposable database. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("surok")
            .withUsername("test")
            .withPassword("test");

    /** Database context. */
    private final DSLContext ctx;

    /**
     * Creates database-backed representation tests.
     *
     * @param ctx Database context
     */
    @Autowired
    SlV1Test(final DSLContext ctx) {
        this.ctx = ctx;
    }

    /** Checks school fields can be printed from PostgreSQL. */
    @Test
    void printsPersistedUnicodeSchool() throws Exception {
        this.ctx.execute("TRUNCATE public.school RESTART IDENTITY CASCADE");
        this.ctx.execute(
            "INSERT INTO public.school (id, name, is_deleted) VALUES (42, 'Школа №42', false)"
        );
        MatcherAssert.assertThat(
            "Versioned school lost its fields",
            new SlV1(new SlPostgres(this.ctx, 42L)).print(new MdJson()).print().toString(),
            Matchers.equalTo("{\"id\":42,\"name\":\"Школа №42\"}")
        );
    }

    /**
     * Configures the disposable database.
     *
     * @param registry Test properties
     */
    @DynamicPropertySource
    private static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.liquibase.enabled", () -> true);
        registry.add("spring.datasource.url", SlV1Test.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", SlV1Test.POSTGRES::getUsername);
        registry.add("spring.datasource.password", SlV1Test.POSTGRES::getPassword);
    }
}
