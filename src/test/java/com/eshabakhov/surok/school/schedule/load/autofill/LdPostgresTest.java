/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.autofill;

import com.eshabakhov.surok.federal.curriculum.CrPostgres;
import com.eshabakhov.surok.federal.subject.SbFake;
import com.eshabakhov.surok.federal.subject.Subject;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.school.schedule.SdPostgres;
import com.eshabakhov.surok.school.schedule.load.hours.WeeklyHours;
import com.eshabakhov.surok.school.schedule.load.hours.WhFixed;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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
 * Automatic mandatory hours and grade additions persisted against real PostgreSQL.
 * For example, newly created classes participate on reopening without explicit selections.
 *
 * @since 0.0.1
 */
@Testcontainers
@DirtiesContext
@SpringBootTest
@Sql(
    statements = {
        "ALTER TABLE class_curriculum DROP CONSTRAINT IF EXISTS test_rejected_hours",
        "TRUNCATE TABLE school, subject, federal_curriculum RESTART IDENTITY CASCADE",
        "TRUNCATE TABLE subject RESTART IDENTITY CASCADE",
        "INSERT INTO school (id, name) VALUES (1, 'Школа'), (2, 'Чужая школа')",
        """
        INSERT INTO schedule (id, school_id, name, is_deleted)
        VALUES (101, 1, 'Год', false), (103, 1, 'Полугодие', false),
            (105, 1, 'Архив', true), (201, 2, 'Чужой год', false);
        """,
        """
        INSERT INTO school_class (id, school_id, grade, litera, is_deleted)
        VALUES (17, 1, 5, 'А', false), (19, 1, 6, 'Б', false),
            (21, 2, 5, 'Чужой', false), (23, 1, 5, 'Удалён', true), (27, 1, 3, 'В', false);
        """,
        """
        INSERT INTO subject (id, school_id, origin, name, is_deleted)
        VALUES (29, NULL, 'FEDERAL', 'Математика', false),
        (31, 1, 'SCHOOL', 'Робототехника', false),
        (37, 1, 'SCHOOL', 'Шахматы', false),
        (33, 2, 'SCHOOL', 'Чужой предмет', false),
        (35, 1, 'SCHOOL', 'Архивный', true);
        """,
        """
        INSERT INTO federal_curriculum
            (id, title, education_level, study_week_type, version, academic_year, is_deleted)
        VALUES (61, 'Основной план', 'BASIC', 'FIVE_DAYS', '1', '2026/2027', false),
            (67, 'Другой план', 'PRIMARY', 'FIVE_DAYS', '2', '2026/2027', false),
            (69, 'Архивный план', 'BASIC', 'FIVE_DAYS', '1', '2026/2027', true);
        """,
        """
        INSERT INTO federal_curriculum_requirement
        (id, federal_curriculum_id, grade, subject_id, hours, part_type)
        VALUES (71, 61, 5, (SELECT id FROM subject WHERE name = 'Математика'
        AND NOT is_deleted), 3, 'MANDATORY'),
        (75, 61, 6, (SELECT id FROM subject WHERE name = 'Математика'
        AND NOT is_deleted), 7, 'MANDATORY'),
        (77, 67, 3, (SELECT id FROM subject WHERE name = 'Математика'
        AND NOT is_deleted), 4, 'MANDATORY');
        """,
        """
        INSERT INTO federal_curriculum_requirement
        (federal_curriculum_id, grade, hours, part_type)
        VALUES (61, 5, 3, 'OPTIONAL'), (61, 6, 2, 'OPTIONAL'), (67, 3, 1, 'OPTIONAL');
        """,
        """
        INSERT INTO class_curriculum (schedule_id, school_class_id, subject_id, hours)
        VALUES (101, 17, 29, 5);
        """
    }
)
final class LdPostgresTest {

    /** Isolated PostgreSQL database for application migrations. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:14-alpine");

    @Test
    void reportsAnAbsentDraftWithoutADatabaseWrapper(@Autowired final DSLContext ctx) {
        Assertions.assertThrows(
            LoadDraftNotFoundException.class,
            () -> new LdPostgres(ctx, new SdPostgres(ctx, 103L)).print(new MdJson()),
            "An absent draft remained hidden inside a database exception"
        );
    }

    @Test
    void automaticallyReplacesMandatoryHoursForAllApplicableClasses(@Autowired final DSLContext ctx)
        throws Exception {
        final LoadDraft draft = new LdPostgres(ctx, new SdPostgres(ctx, 101L));
        draft.save(new CrPostgres(ctx, 61L), Map.of());
        draft.apply();
        MatcherAssert.assertThat(
            "Mandatory hours retained manual overrides or omitted an applicable class",
            ctx.fetch(
                """
                SELECT school_class_id, hours FROM class_curriculum
                ORDER BY school_class_id
                """
            ).map(row -> List.of(row.get("school_class_id"), row.get("hours"))),
            Matchers.contains(List.of(17L, 3), List.of(19L, 7))
        );
    }

    @Test
    void leavesActualLoadsUntouchedWhenSaving(@Autowired final DSLContext ctx) throws Exception {
        new LdPostgres(ctx, new SdPostgres(ctx, 101L)).save(
            new CrPostgres(ctx, 61L), Map.of(5, Map.of(new SbFake(31L), new WhFixed(1)))
        );
        MatcherAssert.assertThat(
            "Saving a draft prematurely changed the actual teaching load",
            ctx.fetch("SELECT hours FROM class_curriculum").getValues(0),
            Matchers.contains(5)
        );
    }

    @Test
    void includesNewClassesInTheSavedGradeSelection(@Autowired final DSLContext ctx)
        throws Exception {
        new LdPostgres(ctx, new SdPostgres(ctx, 101L)).save(
            new CrPostgres(ctx, 61L), Map.of(5, Map.of(new SbFake(31L), new WhFixed(1)))
        );
        ctx.execute(
            "INSERT INTO school_class (id, school_id, grade, litera) VALUES (41, 1, 5, 'Г')"
        );
        new LdPostgres(ctx, new SdPostgres(ctx, 101L)).apply();
        MatcherAssert.assertThat(
            "A newly created class did not inherit its grade's additional hours",
            ctx.fetch(
                """
                SELECT school_class_id FROM class_curriculum
                WHERE subject_id = 31 ORDER BY school_class_id
                """
            ).getValues(0),
            Matchers.contains(17L, 41L)
        );
    }

    @Test
    void restoresGradeAdditionsAfterReopening(@Autowired final DSLContext ctx) throws Exception {
        new LdPostgres(ctx, new SdPostgres(ctx, 101L)).save(
            new CrPostgres(ctx, 61L), Map.of(5, Map.of(new SbFake(31L), new WhFixed(2)))
        );
        new LdPostgres(ctx, new SdPostgres(ctx, 101L)).apply();
        MatcherAssert.assertThat(
            "Reopening lost manually assigned grade hours",
            ctx.fetch(
                """
                SELECT school_class_id, hours FROM class_curriculum
                WHERE subject_id = 31 ORDER BY school_class_id
                """
            ).map(row -> List.of(row.get(0), row.get(1))),
            Matchers.contains(List.of(17L, 2))
        );
    }

    @Test
    void countsIndividualAdditionalLoadsAgainstTheGradeAllowance(@Autowired final DSLContext ctx)
        throws Exception {
        ctx.execute(
            """
            INSERT INTO class_curriculum (schedule_id, school_class_id, subject_id, hours)
            VALUES (101, 17, 37, 2)
            """
        );
        final LoadDraft draft = new LdPostgres(ctx, new SdPostgres(ctx, 101L));
        draft.save(new CrPostgres(ctx, 61L), Map.of(5, Map.of(new SbFake(31L), new WhFixed(1))));
        Assertions.assertThrows(
            DataAccessException.class,
            () -> draft.save(
                new CrPostgres(ctx, 61L), Map.of(5, Map.of(new SbFake(31L), new WhFixed(2)))
            ),
            "An individually assigned subject was omitted from the additional allowance"
        );
    }

    @Test
    void rechecksIndividuallyAddedHoursBeforeApplication(@Autowired final DSLContext ctx)
        throws Exception {
        final LoadDraft draft = new LdPostgres(ctx, new SdPostgres(ctx, 101L));
        draft.save(new CrPostgres(ctx, 61L), Map.of(5, Map.of(new SbFake(31L), new WhFixed(2))));
        ctx.execute(
            """
            INSERT INTO class_curriculum (schedule_id, school_class_id, subject_id, hours)
            VALUES (101, 17, 37, 2)
            """
        );
        Assertions.assertThrows(
            DataAccessException.class, draft::apply,
            "Application ignored additional hours assigned after the draft was saved"
        );
    }

    @ParameterizedTest
    @ValueSource(longs = {29L, 33L, 35L, 999L})
    void rejectsUnavailableOrFederalAdditions(final long id, @Autowired final DSLContext ctx) {
        Assertions.assertThrows(
            DataAccessException.class,
            () -> new LdPostgres(ctx, new SdPostgres(ctx, 101L)).save(
                new CrPostgres(ctx, 61L), Map.of(5, Map.of(new SbFake(id), new WhFixed(1)))
            ),
            "An unavailable or federal subject was accepted as a school addition"
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 4, 10, 12})
    void rejectsAdditionsOutsideThePlanLevel(final int grade, @Autowired final DSLContext ctx) {
        Assertions.assertThrows(
            DataAccessException.class,
            () -> new LdPostgres(ctx, new SdPostgres(ctx, 101L)).save(
                new CrPostgres(ctx, 61L), Map.of(grade, Map.of(new SbFake(31L), new WhFixed(1)))
            ),
            "An additional grade outside the curriculum level was accepted"
        );
    }

    @Test
    void rollsBackAnInvalidReplacement(@Autowired final DSLContext ctx) throws Exception {
        final LoadDraft draft = new LdPostgres(ctx, new SdPostgres(ctx, 101L));
        draft.save(new CrPostgres(ctx, 61L), Map.of(5, Map.of(new SbFake(31L), new WhFixed(1))));
        boolean rejected = false;
        try {
            draft.save(
                new CrPostgres(ctx, 61L), Map.of(5, Map.of(new SbFake(31L), new WhFixed(4)))
            );
        } catch (final DataAccessException ignored) {
            rejected = true;
        }
        new LdPostgres(ctx, new SdPostgres(ctx, 101L)).apply();
        MatcherAssert.assertThat(
            "A rejected replacement lost the saved additional hours",
            List.of(
                rejected,
                ctx.fetch("SELECT hours FROM class_curriculum WHERE subject_id = 31")
                    .getValues(0)
            ),
            Matchers.contains(true, List.of(1))
        );
    }

    @Test
    void switchesTheLevelAndClearsGradeSelections(@Autowired final DSLContext ctx)
        throws Exception {
        final LoadDraft draft = new LdPostgres(ctx, new SdPostgres(ctx, 101L));
        draft.save(new CrPostgres(ctx, 61L), Map.of(5, Map.of(new SbFake(31L), new WhFixed(1))));
        draft.revise(new CrPostgres(ctx, 67L));
        draft.apply();
        MatcherAssert.assertThat(
            "Switching the plan failed to change the class scope or retained grade selections",
            List.of(
                ctx.fetch("SELECT hours FROM load_draft_addition").getValues(0),
                ctx.fetch(
                    "SELECT school_class_id FROM class_curriculum ORDER BY school_class_id"
                ).getValues(0)
            ),
            Matchers.contains(List.of(), List.of(17L, 27L))
        );
    }

    @Test
    void preservesIndividualAdditionsWhenRemovingGradeSelections(@Autowired final DSLContext ctx)
        throws Exception {
        final LoadDraft draft = new LdPostgres(ctx, new SdPostgres(ctx, 101L));
        draft.save(new CrPostgres(ctx, 61L), Map.of(5, Map.of(new SbFake(31L), new WhFixed(1))));
        draft.apply();
        ctx.execute(
            """
            INSERT INTO class_curriculum (schedule_id, school_class_id, subject_id, hours)
            VALUES (101, 17, 37, 2)
            """
        );
        draft.save(new CrPostgres(ctx, 61L), Map.of());
        draft.apply();
        draft.apply();
        MatcherAssert.assertThat(
            "Removing a grade selection removed an unrelated individual additional load",
            ctx.fetch("SELECT subject_id FROM class_curriculum ORDER BY subject_id").getValues(0),
            Matchers.contains(29L, 29L, 37L)
        );
    }

    @ParameterizedTest
    @ValueSource(longs = {105L, 999L})
    void rejectsUnavailableSchedules(final long id, @Autowired final DSLContext ctx) {
        Assertions.assertThrows(
            DataAccessException.class,
            () -> new LdPostgres(ctx, new SdPostgres(ctx, id)).save(
                new CrPostgres(ctx, 61L), Map.of()
            ),
            "An unavailable schedule received a draft"
        );
    }

    @Test
    void serializesConcurrentApplications(@Autowired final DSLContext ctx) throws Exception {
        final LoadDraft draft = new LdPostgres(ctx, new SdPostgres(ctx, 101L));
        draft.save(new CrPostgres(ctx, 61L), Map.of(5, Map.of(new SbFake(31L), new WhFixed(1))));
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            final Future<Boolean> first = executor.submit(
                () -> {
                    draft.apply();
                    return true;
                }
            );
            final Future<Boolean> second = executor.submit(
                () -> {
                    draft.apply();
                    return true;
                }
            );
            first.get(30L, TimeUnit.SECONDS);
            second.get(30L, TimeUnit.SECONDS);
        }
        MatcherAssert.assertThat(
            "Concurrent application duplicated the mandatory or additional loads",
            ctx.fetchSingle("SELECT count(*) FROM class_curriculum").get(0),
            Matchers.is(3L)
        );
    }

    @Test
    void keepsDifferentSubjectsForDifferentGrades(@Autowired final DSLContext ctx)
        throws Exception {
        new LdPostgres(ctx, new SdPostgres(ctx, 101L)).save(
            new CrPostgres(ctx, 61L), Map.of(
                5, Map.of(new SbFake(31L), new WhFixed(1)),
                6, Map.of(new SbFake(37L), new WhFixed(2))
            )
        );
        new LdPostgres(ctx, new SdPostgres(ctx, 101L)).apply();
        MatcherAssert.assertThat(
            "Applying the draft mixed additional subjects or hours between grades",
            ctx.fetch(
                """
                SELECT school_class_id, subject_id, hours FROM class_curriculum
                WHERE subject_id != 29 ORDER BY school_class_id
                """
            ).map(row -> List.of(row.get(0), row.get(1), row.get(2))),
            Matchers.contains(List.of(17L, 31L, 1), List.of(19L, 37L, 2))
        );
    }

    @Test
    void rejectsHoursWithoutAnExplicitMaximum(@Autowired final DSLContext ctx) {
        Assertions.assertThrows(
            DataAccessException.class,
            () -> new LdPostgres(ctx, new SdPostgres(ctx, 101L)).save(
                new CrPostgres(ctx, 61L), Map.of(7, Map.of(new SbFake(31L), new WhFixed(1)))
            ),
            "Additional hours without an explicit maximum were saved for a grade without classes"
        );
    }

    @Test
    void rejectsArchivedSelectionsBeforeChangingAnyLoad(@Autowired final DSLContext ctx)
        throws Exception {
        final LoadDraft draft = new LdPostgres(ctx, new SdPostgres(ctx, 101L));
        draft.save(new CrPostgres(ctx, 61L), Map.of(5, Map.of(new SbFake(31L), new WhFixed(2))));
        ctx.execute("UPDATE subject SET is_deleted = true WHERE id = 31");
        boolean rejected = false;
        try {
            draft.apply();
        } catch (final DataAccessException ignored) {
            rejected = true;
        }
        MatcherAssert.assertThat(
            "An archived selection was accepted or changed mandatory hours before rejection",
            List.of(
                rejected, ctx.fetch("SELECT hours FROM class_curriculum").getValues(0)
            ),
            Matchers.contains(true, List.of(5))
        );
    }

    @Test
    void removesAnArchivedSelection(@Autowired final DSLContext ctx) throws Exception {
        final LoadDraft draft = new LdPostgres(ctx, new SdPostgres(ctx, 101L));
        draft.save(new CrPostgres(ctx, 61L), Map.of(5, Map.of(new SbFake(31L), new WhFixed(2))));
        draft.apply();
        ctx.execute("UPDATE subject SET is_deleted = true WHERE id = 31");
        draft.save(new CrPostgres(ctx, 61L), Map.of(5, Map.of(new SbFake(31L), new WhFixed(0))));
        draft.apply();
        MatcherAssert.assertThat(
            "An archived grade selection retained its actual load after removal",
            ctx.fetch("SELECT subject_id FROM class_curriculum ORDER BY school_class_id")
                .getValues(0),
            Matchers.contains(29L, 29L)
        );
    }

    @Test
    void rejectsArchivedRemovalsForAnotherGrade(@Autowired final DSLContext ctx) throws Exception {
        final LoadDraft draft = new LdPostgres(ctx, new SdPostgres(ctx, 101L));
        draft.save(new CrPostgres(ctx, 61L), Map.of(5, Map.of(new SbFake(31L), new WhFixed(2))));
        ctx.execute("UPDATE subject SET is_deleted = true WHERE id = 31");
        Assertions.assertThrows(
            DataAccessException.class,
            () -> draft.save(
                new CrPostgres(ctx, 61L), Map.of(6, Map.of(new SbFake(31L), new WhFixed(0)))
            ),
            "An archived removal was authorized using another grade's selection"
        );
    }

    @Test
    void rechecksTheMaximumBeforeApplyingSavedHours(@Autowired final DSLContext ctx)
        throws Exception {
        final LoadDraft draft = new LdPostgres(ctx, new SdPostgres(ctx, 101L));
        draft.save(new CrPostgres(ctx, 61L), Map.of(5, Map.of(new SbFake(31L), new WhFixed(3))));
        ctx.execute(
            "UPDATE federal_curriculum_requirement SET hours = 1 WHERE part_type = 'OPTIONAL'"
        );
        Assertions.assertThrows(
            DataAccessException.class, draft::apply,
            "Previously saved hours bypassed a reduced curriculum maximum"
        );
    }

    @Test
    void rollsBackAllGradesWhenOneSubjectIsUnavailable(@Autowired final DSLContext ctx)
        throws Exception {
        final LoadDraft draft = new LdPostgres(ctx, new SdPostgres(ctx, 101L));
        draft.save(new CrPostgres(ctx, 61L), Map.of(5, Map.of(new SbFake(31L), new WhFixed(2))));
        final Map<Integer, Map<Subject, WeeklyHours>> hours =
            new LinkedHashMap<>();
        hours.put(5, Map.of(new SbFake(37L), new WhFixed(1)));
        hours.put(6, Map.of(new SbFake(33L), new WhFixed(1)));
        boolean rejected = false;
        try {
            draft.save(new CrPostgres(ctx, 61L), hours);
        } catch (final DataAccessException ignored) {
            rejected = true;
        }
        MatcherAssert.assertThat(
            "An invalid later grade left partial writes in the draft",
            List.of(
                rejected, ctx.fetch("SELECT subject_id, hours FROM load_draft_addition")
                    .map(row -> List.of(row.get(0), row.get(1)))
            ),
            Matchers.contains(true, List.of(List.of(31L, 2)))
        );
    }

    @ParameterizedTest
    @CsvSource({"0, 1", "3, 2"})
    void printsSelectionsAndUsageAfterTheAllowanceIsExceeded(
        final int maximum, final int selected, @Autowired final DSLContext ctx
    ) throws Exception {
        final LoadDraft draft = new LdPostgres(ctx, new SdPostgres(ctx, 101L));
        draft.save(
            new CrPostgres(ctx, 61L), Map.of(5, Map.of(new SbFake(31L), new WhFixed(selected)))
        );
        ctx.execute(
            """
            INSERT INTO class_curriculum (schedule_id, school_class_id, subject_id, hours)
            VALUES (101, 17, 37, 2)
            """
        );
        ctx.execute(
            "UPDATE federal_curriculum_requirement SET hours = ? WHERE part_type = 'OPTIONAL'",
            maximum
        );
        MatcherAssert.assertThat(
            "The draft representation lost selected hours, individual usage or the reduced maximum",
            new LdPostgres(ctx, new SdPostgres(ctx, 101L))
                .print(new MdJson()).print().path("additions").path(0),
            Matchers.equalTo(
                JsonNodeFactory.instance.objectNode().put("grade", 5).setAll(
                    Map.of(
                        "requirements", JsonNodeFactory.instance.arrayNode().add(
                            JsonNodeFactory.instance.objectNode().put("id", 1L)
                                .put("grade", 5).put("hours", maximum)
                                .put("partType", "OPTIONAL")
                        ),
                        "subjects", JsonNodeFactory.instance.arrayNode().add(
                            JsonNodeFactory.instance.objectNode().put("id", 31L)
                                .put("name", "Робототехника").put("origin", "SCHOOL")
                                .put("hours", selected)
                        ),
                        "classes", JsonNodeFactory.instance.arrayNode().add(
                            JsonNodeFactory.instance.objectNode().put("id", 17L)
                                .put("name", "5А").put("grade", 5).put("litera", "А")
                                .<ObjectNode>set(
                                    "hours", JsonNodeFactory.instance.objectNode()
                                        .put("current", 2).put("planned", 2 + selected)
                                )
                        )
                    )
                )
            )
        );
    }

    @Test
    void cannotAssignMandatorySubjectsAsGradeAdditions(@Autowired final DSLContext ctx) {
        Assertions.assertThrows(
            DataAccessException.class,
            () -> new LdPostgres(ctx, new SdPostgres(ctx, 101L)).save(
                new CrPostgres(ctx, 61L), Map.of(5, Map.of(new SbFake(29L), new WhFixed(2)))
            ),
            "The draft counted one subject as both mandatory and additional in the same grade"
        );
    }

    @Test
    void cannotAssignFederalSubjectsOutsideThePlanAsAdditions(@Autowired final DSLContext ctx) {
        ctx.execute("DELETE FROM federal_curriculum_requirement WHERE id = 75");
        Assertions.assertThrows(
            DataAccessException.class,
            () -> new LdPostgres(ctx, new SdPostgres(ctx, 101L)).save(
                new CrPostgres(ctx, 61L), Map.of(6, Map.of(new SbFake(29L), new WhFixed(2)))
            ),
            "A federal subject became an additional school subject outside its plan grade"
        );
    }

    @Test
    void appliesOnlyThePartWhoseSubjectsAreAvailable(@Autowired final DSLContext ctx)
        throws Exception {
        ctx.execute(
            "UPDATE subject SET origin = 'FEDERAL', school_id = NULL, is_deleted = true WHERE id = 37"
        );
        ctx.execute(
            """
            INSERT INTO federal_curriculum_requirement
                (federal_curriculum_id, grade, subject_id, hours, part_type)
            VALUES (61, 5, 37, 2, 'MANDATORY')
            """
        );
        final LoadDraft draft = new LdPostgres(ctx, new SdPostgres(ctx, 101L));
        draft.save(new CrPostgres(ctx, 61L), Map.of());
        draft.apply();
        MatcherAssert.assertThat(
            "Partial application created a load for an archived subject or skipped available loads",
            ctx.fetch(
                "SELECT subject_id, hours FROM class_curriculum ORDER BY school_class_id"
            ).map(row -> List.of(row.get(0), row.get(1))),
            Matchers.contains(List.of(29L, 3), List.of(29L, 7))
        );
    }

    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.liquibase.enabled", () -> true);
        registry.add("spring.datasource.url", LdPostgresTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", LdPostgresTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", LdPostgresTest.POSTGRES::getPassword);
    }
}
