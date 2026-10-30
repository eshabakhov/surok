/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement;

import com.eshabakhov.surok.federal.curriculum.requirement.input.RequirementInputException;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInputHttp;
import com.eshabakhov.surok.media.MdJson;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Optional requirements persist a grade maximum without a subject.
 * For example, revising the maximum to zero explicitly prohibits additional hours.
 *
 * @since 0.0.1
 */
@Testcontainers
@DirtiesContext
@SpringBootTest
@Sql(
    statements = {
        "TRUNCATE TABLE federal_curriculum RESTART IDENTITY CASCADE",
        """
        INSERT INTO subject (id, name, origin) VALUES (504, 'Логика №71', 'FEDERAL')
        ON CONFLICT (id) DO NOTHING;
        """,
        """
        INSERT INTO federal_curriculum
            (id, title, education_level, study_week_type, version, academic_year)
        VALUES (61, 'Основной план', 'BASIC', 'FIVE_DAYS', '1', '2026/2027');
        """,
        """
        INSERT INTO federal_curriculum_requirement
        (id, federal_curriculum_id, grade, hours, part_type)
        VALUES (71, 61, 5, 3, 'OPTIONAL');
        """
    }
)
final class RqOptionalTest {

    /** Database including the application schema. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:14-alpine");

    @Test
    void printsTheMaximumWithoutSubjectFields(@Autowired final DSLContext ctx) throws Exception {
        MatcherAssert.assertThat(
            "The optional requirement exposed subject hours or lost its maximum",
            new RqOptional(ctx, 71L).print(new MdJson()).print(),
            Matchers.is(
                JsonNodeFactory.instance.objectNode().put("id", 71L).put("grade", 5)
                    .put("hours", 3).put("partType", "OPTIONAL")
            )
        );
    }

    @Test
    void revisesTheMaximumToAnExplicitZero(@Autowired final DSLContext ctx) throws Exception {
        new RqOptional(ctx, 71L).revise(
            new RqInputHttp(Map.of("grade", "5", "partType", "OPTIONAL", "hours", "0"))
        );
        MatcherAssert.assertThat(
            "Revision failed to persist an explicit zero maximum",
            ctx.fetchValue(
                "SELECT hours FROM federal_curriculum_requirement WHERE id = 71"
            ),
            Matchers.is(0)
        );
    }

    @ParameterizedTest
    @CsvSource({"4, 3", "10, 3", "5, -1", "5, 1.5"})
    void rejectsInvalidFields(
        final String grade, final String maximum, @Autowired final DSLContext ctx
    ) {
        Assertions.assertThrows(
            RequirementInputException.class,
            () -> new RqOptional(ctx, 71L).revise(
                new RqInputHttp(
                    Map.of("grade", grade, "partType", "OPTIONAL", "hours", maximum)
                )
            ),
            "An invalid maximum or out-of-level grade was accepted"
        );
    }

    @Test
    void rejectsSubjectFields(@Autowired final DSLContext ctx) {
        Assertions.assertThrows(
            RequirementInputException.class,
            () -> new RqOptional(ctx, 71L).revise(
                new RqInputHttp(
                    Map.of(
                        "grade", "5", "partType", "OPTIONAL", "hours", "3",
                        "subjectName", "Робототехника"
                    )
                )
            ),
            "The optional requirement accepted a specific subject"
        );
    }

    @Test
    void rejectsDuplicateGradeRequirements(@Autowired final DSLContext ctx) {
        Assertions.assertThrows(
            DataIntegrityViolationException.class,
            () -> ctx.execute(
                """
                INSERT INTO federal_curriculum_requirement
                (federal_curriculum_id, grade, hours, part_type)
                VALUES (61, 5, 7, 'OPTIONAL')
                """
            ),
            "The database accepted two active optional requirements for one grade"
        );
    }

    @Test
    void rejectsIncompatibleFieldsInTheDatabase(@Autowired final DSLContext ctx) {
        Assertions.assertThrows(
            DataIntegrityViolationException.class,
            () -> ctx.execute(
                "UPDATE federal_curriculum_requirement SET subject_id = 504 WHERE id = 71"
            ),
            "The database accepted a subject in an optional requirement"
        );
    }

    @Test
    void permitsRecreationAfterDeletion(@Autowired final DSLContext ctx) throws Exception {
        new RqsPostgres(ctx, 61L).remove(71L);
        MatcherAssert.assertThat(
            "A deleted requirement prevented configuring a new grade maximum",
            new RqsPostgres(ctx, 61L).create(
                new RqInputHttp(Map.of("grade", "5", "partType", "OPTIONAL", "hours", "7"))
            ),
            Matchers.instanceOf(RqOptional.class)
        );
    }

    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.liquibase.enabled", () -> true);
        registry.add("spring.datasource.url", RqOptionalTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", RqOptionalTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", RqOptionalTest.POSTGRES::getPassword);
    }
}
