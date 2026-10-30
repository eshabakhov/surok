/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement;

import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInput;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInputGrade;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInputHours;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInputPart;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInputSubject;
import com.eshabakhov.surok.media.MdJson;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Test for {@link Requirements}.
 *
 * @since 0.0.1
 * @checkstyle ProhibitFieldsInTestClassesCheck (1000 lines)
 * @checkstyle NoJavadocForPrivateMethodsCheck (1000 lines)
 */
@Testcontainers
@DirtiesContext
@SpringBootTest
@Sql(
    statements = """
        INSERT INTO subject (id, origin, name)
        VALUES (514, 'FEDERAL', 'Математика')
        ON CONFLICT DO NOTHING;
        """
)
@SuppressWarnings({"PMD.UnusedPrivateMethod", "UnusedMethod"})
final class RequirementsTest {

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
    RequirementsTest(final DSLContext ctx) {
        this.ctx = ctx;
    }

    @Test
    void revisesSelectedRequirement() throws Exception {
        MatcherAssert.assertThat(
            "Revision must be delegated to the selected requirement",
            this.prepare(7L).revise(
                7L, this.input("7")
            ).print(new MdJson()).print(),
            Matchers.is(
                JsonNodeFactory.instance.objectNode()
                    .put("id", 7L)
                    .put("grade", 7).<ObjectNode>set(
                        "subject", JsonNodeFactory.instance.objectNode().put("id", 514L)
                            .put("name", "Математика").put("origin", "FEDERAL")
                    ).put("hours", 6)
                    .put("partType", "MANDATORY")
            )
        );
    }

    /**
     * Prepares a curriculum and an optional persisted requirement.
     *
     * @param id Requirement identity, or zero for an empty curriculum
     * @return Persisted requirements
     */
    private Requirements prepare(final long id) {
        this.ctx.execute("TRUNCATE public.federal_curriculum RESTART IDENTITY CASCADE");
        this.ctx.execute(
            """
            INSERT INTO public.federal_curriculum
            (id, title, education_level, study_week_type, version, academic_year, is_deleted)
            VALUES (1, 'План №1', 'BASIC', 'FIVE_DAYS', '1', '2026/2027', false)
            """
        );
        if (id > 0L) {
            this.ctx.execute(
                """
                INSERT INTO public.federal_curriculum_requirement
                (id, federal_curriculum_id, grade, subject_id, hours,
                part_type, is_deleted)
                VALUES (?, 1, 5, (SELECT id FROM subject WHERE name = 'Математика'
                AND NOT is_deleted), 6, 'MANDATORY', false)
                """,
                id
            );
        }
        return new RqsPostgres(this.ctx, 1L);
    }

    /**
     * Builds complete requirement input.
     *
     * @param grade School grade
     * @return Valid requirement input
     */
    private RequirementInput input(final String grade) {
        return new RqInputPart(
            new RqInputHours(
                new RqInputSubject(new RqInputGrade(new RqInput(), grade), "514"),
                "6"
            ),
            "MANDATORY"
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
        registry.add("spring.datasource.url", RequirementsTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", RequirementsTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", RequirementsTest.POSTGRES::getPassword);
    }
}
