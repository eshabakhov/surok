/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement;

import com.eshabakhov.surok.federal.curriculum.requirement.input.RequirementInputException;
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
import org.jooq.exception.NoDataFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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
 * Test for {@link RqMandatory}.
 *
 * @since 0.0.1
 * @checkstyle ProhibitFieldsInTestClassesCheck (1000 lines)
 */
@Testcontainers
@DirtiesContext
@SpringBootTest
@Sql(
    statements = """
        INSERT INTO subject (id, origin, name)
        VALUES (511, 'FEDERAL', 'Physics'),
        (507, 'FEDERAL', 'History'),
        (504, 'FEDERAL', 'Mathematics')
        ON CONFLICT DO NOTHING;
        """
)
@SuppressWarnings({"PMD.UnusedPrivateMethod", "UnusedMethod"})
final class RqMandatoryTest {

    /** Postgres container. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("surok")
            .withUsername("test")
            .withPassword("test");

    /** JOOQ context. */
    private final DSLContext ctx;

    @Autowired
    RqMandatoryTest(final DSLContext ctx) {
        this.ctx = ctx;
    }

    @BeforeEach
    void insertRequirement() {
        this.ctx.resultQuery(
            """
                INSERT INTO public.federal_curriculum (id, title, education_level, study_week_type,
                version, academic_year, is_deleted)
                VALUES (?, ?, CAST(? AS education_level_type), CAST(? AS study_week_type), ?, ?, ?)
                """,
            1L,
            "Plan",
            "BASIC",
            "FIVE_DAYS",
            "1",
            "2026/2027",
            false
        ).execute();
        this.ctx.resultQuery(
            """
                INSERT INTO public.federal_curriculum_requirement (id, federal_curriculum_id, grade,
                subject_id, hours, part_type, is_deleted)
                VALUES (?, ?, ?, (SELECT id FROM subject WHERE name = ?
                AND NOT is_deleted), ?, CAST(? AS curriculum_part_type), ?)
                """,
            7L,
            1L,
            1,
            "Mathematics",
            1,
            "MANDATORY",
            false
        ).execute();
    }

    @AfterEach
    void truncateCurriculums() {
        this.ctx.resultQuery(
            """
                TRUNCATE TABLE public.federal_curriculum RESTART IDENTITY CASCADE
                """
        ).execute();
    }

    @Test
    void exposesIdentity() {
        MatcherAssert.assertThat(
            "Postgres requirement must expose its identity",
            new RqMandatory(this.ctx, 7L).uid(),
            Matchers.is(7L)
        );
    }

    @Test
    void printsRequirement() throws Exception {
        MatcherAssert.assertThat(
            "Postgres requirement must print persisted fields",
            new RqMandatory(this.ctx, 7L).print(new MdJson()).print(),
            Matchers.is(
                JsonNodeFactory.instance.objectNode()
                    .put("id", 7L)
                    .put("grade", 1).<ObjectNode>set(
                        "subject", JsonNodeFactory.instance.objectNode().put("id", 504L)
                            .put("name", "Mathematics").put("origin", "FEDERAL")
                    ).put("hours", 1)
                    .put("partType", "MANDATORY")
            )
        );
    }

    @Test
    void revisesRequirement() throws Exception {
        MatcherAssert.assertThat(
            "Revision must preserve identity and update every field",
            new RqMandatory(this.ctx, 7L).revise(
                new RqInputPart(
                    new RqInputHours(
                        new RqInputSubject(
                            new RqInputGrade(new RqInput(), "9"),
                            "511"
                        ),
                        "2"
                    ),
                    "MANDATORY"
                )
            ).print(new MdJson()).print(),
            Matchers.is(
                JsonNodeFactory.instance.objectNode()
                    .put("id", 7L)
                    .put("grade", 9).<ObjectNode>set(
                        "subject", JsonNodeFactory.instance.objectNode().put("id", 511L)
                            .put("name", "Physics").put("origin", "FEDERAL")
                    ).put("hours", 2)
                    .put("partType", "MANDATORY")
            )
        );
    }

    @Test
    void rejectsRevisingMissingRequirements() {
        Assertions.assertThrows(
            RequirementInputException.class,
            () -> new RqMandatory(this.ctx, 99L).revise(
                new RqInputPart(
                    new RqInputHours(
                        new RqInputSubject(new RqInputGrade(new RqInput(), "5"), "507"),
                        "2"
                    ),
                    "MANDATORY"
                )
            ),
            "Revising a missing requirement returned a subject with no identity"
        );
    }

    @Test
    void rejectsPrintingMissingRequirement() {
        Assertions.assertThrows(
            NoDataFoundException.class,
            () -> new RqMandatory(this.ctx, 99L).print(new MdJson())
        );
    }

    @Test
    void rejectsNullRevisionInput() {
        Assertions.assertThrows(
            NullPointerException.class,
            () -> new RqMandatory(this.ctx, 7L).revise(null)
        );
    }

    @DynamicPropertySource
    private static void properties(final DynamicPropertyRegistry registry) {
        registry.add(
            "spring.datasource.driver-class-name",
            () -> "org.postgresql.Driver"
        );
        registry.add(
            "spring.datasource.url",
            RqMandatoryTest.POSTGRES::getJdbcUrl
        );
        registry.add(
            "spring.datasource.username",
            RqMandatoryTest.POSTGRES::getUsername
        );
        registry.add(
            "spring.datasource.password",
            RqMandatoryTest.POSTGRES::getPassword
        );
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
