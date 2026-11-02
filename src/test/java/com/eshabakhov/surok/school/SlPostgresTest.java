/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school;

import com.eshabakhov.surok.federal.subject.SbsPostgres;
import com.eshabakhov.surok.school.building.BdsPostgres;
import com.eshabakhov.surok.school.schedule.SdsPostgres;
import com.eshabakhov.surok.school.schoolclass.ScsPostgres;
import com.eshabakhov.surok.school.teacher.ThsPostgres;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
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
 * Test for {@link SlPostgres}.
 *
 * @since 0.0.1
 * @checkstyle ProhibitFieldsInTestClassesCheck (1000 lines)
 */
@Testcontainers
@DirtiesContext
@SpringBootTest
@SuppressWarnings({"PMD.UnusedPrivateMethod", "UnusedMethod"})
final class SlPostgresTest {

    /** Postgres container. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("surok")
            .withUsername("test")
            .withPassword("test");

    /** JOOQ DSL context for executing database queries. */
    private final DSLContext ctx;

    @Autowired
    SlPostgresTest(final DSLContext ctx) {
        this.ctx = ctx;
    }

    @AfterEach
    void truncateSchool() {
        this.ctx.resultQuery(
            """
                TRUNCATE TABLE public.school, subject CASCADE
                """
        ).execute();
    }

    @Test
    void fetchBuildings() {
        this.ctx.resultQuery(
            """
                INSERT INTO public.school (id, name, is_deleted)
                VALUES (?, ?, ?)
                """,
            1L,
            "Cab school",
            false
        ).execute();
        Assertions.assertEquals(
            new BdsPostgres(
                this.ctx,
                1L
            ),
            new SlPostgres(
                this.ctx,
                1L
            ).buildings()
        );
    }

    @Test
    void fetchTeachers() {
        this.ctx.resultQuery(
            """
                INSERT INTO public.school (id, name, is_deleted)
                VALUES (?, ?, ?)
                """,
            1L,
            "Tea school",
            false
        ).execute();
        Assertions.assertEquals(
            new ThsPostgres(
                this.ctx,
                1L
            ),
            new SlPostgres(
                this.ctx,
                1L
            ).teachers()
        );
    }

    @Test
    void fetchSchoolClasses() {
        this.ctx.resultQuery(
            """
                INSERT INTO public.school (id, name, is_deleted)
                VALUES (?, ?, ?)
                """,
            1L,
            "Cla school",
            false
        ).execute();
        Assertions.assertEquals(
            new ScsPostgres(
                this.ctx,
                1L
            ),
            new SlPostgres(
                this.ctx,
                1L
            ).schoolClasses()
        );
    }

    @Test
    void fetchSubjects() {
        this.ctx.resultQuery(
            """
                INSERT INTO public.school (id, name, is_deleted)
                VALUES (?, ?, ?)
                """,
            1L,
            "Sub school",
            false
        ).execute();
        Assertions.assertEquals(
            new SbsPostgres(
                this.ctx,
                1L
            ),
            new SlPostgres(
                this.ctx,
                1L
            ).subjects()
        );
    }

    @Test
    void fetchSchedules() {
        this.ctx.resultQuery(
            """
                INSERT INTO public.school (id, name, is_deleted)
                VALUES (?, ?, ?)
                """,
            1L,
            "Sch school",
            false
        ).execute();
        Assertions.assertEquals(
            new SdsPostgres(
                this.ctx,
                1L
            ),
            new SlPostgres(
                this.ctx,
                1L
            ).schedules()
        );
    }

    @DynamicPropertySource
    private static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.datasource.url", SlPostgresTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", SlPostgresTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", SlPostgresTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
