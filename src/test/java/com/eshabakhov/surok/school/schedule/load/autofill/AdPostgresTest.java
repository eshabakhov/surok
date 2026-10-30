/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.autofill;

import com.eshabakhov.surok.federal.curriculum.CrPostgres;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RequirementInputException;
import com.eshabakhov.surok.federal.subject.SbFake;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.school.schedule.SdPostgres;
import com.eshabakhov.surok.school.schedule.load.hours.WhFixed;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Assertions;
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
 * One grade's additional selections, usage and maximum.
 * For example, omitted subjects retain zero hours until the draft is applied.
 *
 * @since 0.0.1
 */
@Testcontainers
@DirtiesContext
@SpringBootTest
@Sql(
    statements = {
        "TRUNCATE TABLE school, subject, federal_curriculum RESTART IDENTITY CASCADE",
        "INSERT INTO school (id, name) VALUES (13, 'Школа №13'), (19, 'Другая школа')",
        "INSERT INTO schedule (id, school_id, name) VALUES (107, 13, 'Учебный год')",
        """
        INSERT INTO school_class (id, school_id, grade, litera, is_deleted)
        VALUES (23, 13, 5, 'Б', false), (25, 13, 5, 'В', false), (27, 13, 6, 'А', false),
            (29, 13, 3, 'А', false), (31, 13, 7, 'А', true), (33, 19, 7, 'А', false);
        """,
        """
        INSERT INTO subject (id, school_id, origin, name)
        VALUES (37, 13, 'SCHOOL', 'Робототехника'), (39, 13, 'SCHOOL', 'Шахматы'),
            (41, 19, 'SCHOOL', 'Чужой предмет'), (43, NULL, 'FEDERAL', 'Математика');
        """,
        """
        INSERT INTO federal_curriculum
            (id, title, education_level, study_week_type, version, academic_year)
        VALUES (67, 'Основной план', 'BASIC', 'FIVE_DAYS', '1', '2026/2027');
        """,
        """
        INSERT INTO federal_curriculum_requirement
            (id, federal_curriculum_id, grade, hours, part_type)
        VALUES (73, 67, 5, 3, 'OPTIONAL'), (79, 67, 6, 2, 'OPTIONAL');
        """,
        "INSERT INTO load_draft (schedule_id, curriculum_id) VALUES (107, 67)",
        """
        INSERT INTO class_curriculum (schedule_id, school_class_id, subject_id, hours)
        VALUES (107, 23, 39, 2);
        """
    }
)
final class AdPostgresTest {

    /** Isolated PostgreSQL database for application migrations. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:14-alpine");

    @Test
    void keepsZeroHoursForOmittedSubjects(@Autowired final DSLContext ctx) throws Exception {
        final Addition addition = new AdPostgres(
            ctx, new SdPostgres(ctx, 107L), new CrPostgres(ctx, 67L), 5
        );
        addition.save(Map.of(new SbFake(37L), new WhFixed(1)));
        addition.save(Map.of());
        MatcherAssert.assertThat(
            "An omitted subject lost its pending removal or retained its planned hours",
            ctx.fetch("SELECT subject_id, hours FROM load_draft_addition")
                .map(row -> List.of(row.get(0), row.get(1))),
            Matchers.contains(List.of(37L, 0))
        );
    }

    @Test
    void countsIndividualLoadsWhenCheckingTheMaximum(@Autowired final DSLContext ctx)
        throws Exception {
        final Addition addition = new AdPostgres(
            ctx, new SdPostgres(ctx, 107L), new CrPostgres(ctx, 67L), 5
        );
        addition.save(Map.of(new SbFake(37L), new WhFixed(1)));
        ctx.execute("UPDATE class_curriculum SET hours = 3");
        Assertions.assertThrows(
            RequirementInputException.class, addition::check,
            "The grade maximum ignored an individual additional load"
        );
    }

    @Test
    void rejectsSubjectsFromAnotherSchool(@Autowired final DSLContext ctx) {
        Assertions.assertThrows(
            LoadInputException.class,
            () -> new AdPostgres(
                ctx, new SdPostgres(ctx, 107L), new CrPostgres(ctx, 67L), 5
            ).save(Map.of(new SbFake(41L), new WhFixed(1))),
            "A foreign subject was accepted into the grade addition"
        );
    }

    @Test
    void printsSelectionsAndUsageDespiteAReducedMaximum(@Autowired final DSLContext ctx)
        throws Exception {
        final Addition addition = new AdPostgres(
            ctx, new SdPostgres(ctx, 107L), new CrPostgres(ctx, 67L), 5
        );
        addition.save(Map.of(new SbFake(37L), new WhFixed(1)));
        ctx.execute("UPDATE federal_curriculum_requirement SET hours = 0 WHERE id = 73");
        MatcherAssert.assertThat(
            "The addition lost its selected subject, class totals or reduced requirement",
            addition.print(new MdJson()).print(),
            Matchers.equalTo(
                JsonNodeFactory.instance.objectNode().put("grade", 5).setAll(
                    Map.of(
                        "requirements", JsonNodeFactory.instance.arrayNode().add(
                            JsonNodeFactory.instance.objectNode().put("id", 73L)
                                .put("grade", 5).put("hours", 0).put("partType", "OPTIONAL")
                        ),
                        "subjects", JsonNodeFactory.instance.arrayNode().add(
                            JsonNodeFactory.instance.objectNode().put("id", 37L)
                                .put("name", "Робототехника").put("origin", "SCHOOL")
                                .put("hours", 1)
                        ),
                        "classes", JsonNodeFactory.instance.arrayNode().add(
                            JsonNodeFactory.instance.objectNode().put("id", 23L)
                                .put("name", "5Б").put("grade", 5).put("litera", "Б")
                                .<ObjectNode>set(
                                    "hours", JsonNodeFactory.instance.objectNode()
                                        .put("current", 2).put("planned", 3)
                                )
                        ).add(
                            JsonNodeFactory.instance.objectNode().put("id", 25L)
                                .put("name", "5В").put("grade", 5).put("litera", "В")
                                .<ObjectNode>set(
                                    "hours", JsonNodeFactory.instance.objectNode()
                                        .put("current", 0).put("planned", 1)
                                )
                        )
                    )
                )
            )
        );
    }

    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.liquibase.enabled", () -> true);
        registry.add("spring.datasource.url", AdPostgresTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", AdPostgresTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", AdPostgresTest.POSTGRES::getPassword);
    }
}
