/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.autofill;

import com.eshabakhov.surok.federal.curriculum.CrPostgres;
import com.eshabakhov.surok.federal.subject.SbFake;
import com.eshabakhov.surok.school.schedule.SdPostgres;
import com.eshabakhov.surok.school.schedule.load.hours.WhFixed;
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
 * Grade additions selected and replaced within a draft.
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
final class AdsPostgresTest {

    /** Isolated PostgreSQL database for application migrations. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:14-alpine");

    @Test
    void iteratesOnlyGradesWithApplicableActiveClasses(@Autowired final DSLContext ctx)
        throws Exception {
        final SdPostgres schedule = new SdPostgres(ctx, 107L);
        final CrPostgres curriculum = new CrPostgres(ctx, 67L);
        MatcherAssert.assertThat(
            "The additions included duplicate, archived, foreign or inapplicable grades",
            new AdsPostgres(ctx, schedule, curriculum).iterate(),
            Matchers.contains(
                new AdPostgres(ctx, schedule, curriculum, 5),
                new AdPostgres(ctx, schedule, curriculum, 6)
            )
        );
    }

    @Test
    void savesDifferentSubjectsForEachGrade(@Autowired final DSLContext ctx) throws Exception {
        new AdsPostgres(ctx, new SdPostgres(ctx, 107L), new CrPostgres(ctx, 67L)).save(
            Map.of(
                5, Map.of(new SbFake(37L), new WhFixed(1)),
                6, Map.of(new SbFake(39L), new WhFixed(2))
            )
        );
        MatcherAssert.assertThat(
            "The additions lost their grade-specific subjects or hours",
            ctx.fetch("SELECT grade, subject_id, hours FROM load_draft_addition ORDER BY grade")
                .map(row -> List.of(row.get(0), row.get(1), row.get(2))),
            Matchers.contains(List.of(5, 37L, 1), List.of(6, 39L, 2))
        );
    }

    @Test
    void clearsOmittedGradesEvenAfterTheirClassesAreRemoved(@Autowired final DSLContext ctx)
        throws Exception {
        final Additions additions = new AdsPostgres(
            ctx, new SdPostgres(ctx, 107L), new CrPostgres(ctx, 67L)
        );
        additions.save(Map.of(5, Map.of(new SbFake(37L), new WhFixed(1))));
        ctx.execute("UPDATE school_class SET is_deleted = true WHERE grade = 5");
        additions.save(Map.of());
        MatcherAssert.assertThat(
            "An omitted grade retained its planned hours after its classes were removed",
            ctx.fetchSingle("SELECT hours FROM load_draft_addition").get(0),
            Matchers.is(0)
        );
    }

    @Test
    void rejectsGradesOutsideTheCurriculum(@Autowired final DSLContext ctx) {
        Assertions.assertThrows(
            LoadInputException.class,
            () -> new AdsPostgres(
                ctx, new SdPostgres(ctx, 107L), new CrPostgres(ctx, 67L)
            ).save(Map.of(3, Map.of(new SbFake(37L), new WhFixed(1)))),
            "An inapplicable grade was accepted into the additions"
        );
    }

    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.liquibase.enabled", () -> true);
        registry.add("spring.datasource.url", AdsPostgresTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", AdsPostgresTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", AdsPostgresTest.POSTGRES::getPassword);
    }
}
