/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement;

import com.eshabakhov.surok.Sort;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RequirementInputException;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInput;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInputGrade;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInputHours;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInputPart;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInputSubject;
import com.eshabakhov.surok.filter.FilterRequest;
import com.eshabakhov.surok.filter.FiltersRequest;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.media.MdXml;
import com.eshabakhov.surok.page.PageRequest;
import com.eshabakhov.surok.sort.SortRequest;
import com.eshabakhov.surok.sort.SortsRequest;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.StreamSupport;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockResult;
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
 * Test for {@link RqsPostgres}.
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
        VALUES (501, 'FEDERAL', 'Literature'),
        (502, 'FEDERAL', 'Algebra'),
        (503, 'FEDERAL', 'Geometry'),
        (504, 'FEDERAL', 'Mathematics'),
        (505, 'FEDERAL', 'Biology'),
        (506, 'FEDERAL', 'Art'),
        (507, 'FEDERAL', 'History'),
        (508, 'FEDERAL', 'Chemistry'),
        (509, 'FEDERAL', 'Informatics'),
        (510, 'FEDERAL', 'Music'),
        (511, 'FEDERAL', 'Physics'),
        (512, 'FEDERAL', 'Geography')
        ON CONFLICT DO NOTHING;
        """
)
@SuppressWarnings({"PMD.UnusedPrivateMethod", "UnusedMethod"})
final class RqsPostgresTest {

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
    RqsPostgresTest(final DSLContext ctx) {
        this.ctx = ctx;
    }

    @BeforeEach
    void insertCurriculums() {
        for (long id = 1L; id <= 2L; ++id) {
            this.ctx.resultQuery(
                """
                INSERT INTO public.federal_curriculum (id, title, education_level, study_week_type,
                version, academic_year, is_deleted)
                VALUES (?, ?, CAST(? AS education_level_type), CAST(? AS study_week_type), ?, ?, ?)
                """,
                id,
                String.format("Plan %d", id),
                "BASIC",
                "FIVE_DAYS",
                "1",
                "2026/2027",
                false
            ).execute();
        }
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
    void createsBoundaryRequirement() throws Exception {
        MatcherAssert.assertThat(
            "Created requirement must contain persisted boundary values",
            new RqsPostgres(this.ctx, 1L).create(
                new RqInputPart(
                    new RqInputHours(
                        new RqInputSubject(
                            new RqInputGrade(new RqInput(), "5"),
                            "501"
                        ),
                        "1"
                    ),
                    "MANDATORY"
                )
            ).print(new MdJson()).print(),
            Matchers.equalTo(
                JsonNodeFactory.instance.objectNode()
                    .put("id", 1L)
                    .put("grade", 5).put("hours", 1).put("partType", "MANDATORY").set(
                        "subject", JsonNodeFactory.instance.objectNode().put("id", 501L)
                            .put("name", "Literature").put("origin", "FEDERAL")
                    )
            )
        );
    }

    @Test
    void rejectsDuplicateRequirement() throws Exception {
        final RequirementInput input = new RqInputPart(
            new RqInputHours(
                new RqInputSubject(
                    new RqInputGrade(new RqInput(), "5"),
                    "504"
                ),
                "5"
            ),
            "MANDATORY"
        );
        new RqsPostgres(this.ctx, 1L).create(input);
        MatcherAssert.assertThat(
            "Transaction failure must retain the domain conflict",
            Assertions.assertThrows(
                DataAccessException.class,
                () -> new RqsPostgres(this.ctx, 1L).create(input)
            ).getCause(),
            Matchers.instanceOf(RequirementAlreadyExistsException.class)
        );
    }

    @Test
    void permitsSameRequirementInAnotherCurriculum() throws Exception {
        final RequirementInput input = new RqInputPart(
            new RqInputHours(
                new RqInputSubject(
                    new RqInputGrade(new RqInput(), "5"),
                    "504"
                ),
                "5"
            ),
            "MANDATORY"
        );
        new RqsPostgres(this.ctx, 1L).create(input);
        MatcherAssert.assertThat(
            "Another curriculum must accept the same requirement",
            new RqsPostgres(this.ctx, 2L).create(input).uid(),
            Matchers.notNullValue()
        );
    }

    @Test
    void rejectsNullCreationInput() {
        Assertions.assertThrows(
            NullPointerException.class,
            () -> new RqsPostgres(this.ctx, 1L).create(null)
        );
    }

    // @checkstyle LambdaBodyLengthCheck (50 lines)
    @Test
    void rejectsEmptyInsertResult() {
        MatcherAssert.assertThat(
            "Empty insert result must retain the creation failure",
            Assertions.assertThrows(
                DataAccessException.class,
                () -> new RqsPostgres(
                    DSL.using(
                        new MockConnection(
                            context -> new MockResult[] {
                                new MockResult(
                                    0,
                                    DSL.using(SQLDialect.POSTGRES).newResult()
                                ),
                            }
                        ),
                        SQLDialect.POSTGRES
                    ),
                    1L
                ).create(
                    new RqInputPart(
                        new RqInputHours(
                            new RqInputSubject(
                                new RqInputGrade(new RqInput(), "5"),
                                "504"
                            ),
                            "5"
                        ),
                        "MANDATORY"
                    )
                )
            ).getCause(),
            Matchers.instanceOf(RequirementInputException.class)
        );
    }

    @Test
    void findsRequirement() throws Exception {
        this.ctx.resultQuery(
            """
                INSERT INTO public.federal_curriculum_requirement (id, federal_curriculum_id, grade,
                subject_id, hours, part_type, is_deleted)
                VALUES (?, ?, ?, (SELECT id FROM subject WHERE name = ?
                AND NOT is_deleted), ?, CAST(? AS curriculum_part_type), ?)
                """,
            17L,
            1L,
            6,
            "History",
            2,
            "MANDATORY",
            false
        ).execute();
        MatcherAssert.assertThat(
            "Lookup must return requirement from the current curriculum",
            new RqsPostgres(this.ctx, 1L).requirement(17L).uid(),
            Matchers.is(17L)
        );
    }

    @Test
    void rejectsMissingRequirement() {
        Assertions.assertThrows(
            RequirementNotFoundException.class,
            () -> new RqsPostgres(this.ctx, 1L).requirement(404L)
        );
    }

    @Test
    void rejectsRequirementFromAnotherCurriculum() throws Exception {
        final Requirement created = new RqsPostgres(this.ctx, 2L).create(
            new RqInputPart(
                new RqInputHours(
                    new RqInputSubject(
                        new RqInputGrade(new RqInput(), "7"),
                        "505"
                    ),
                    "2"
                ),
                "MANDATORY"
            )
        );
        Assertions.assertThrows(
            RequirementNotFoundException.class,
            () -> new RqsPostgres(this.ctx, 1L)
                .requirement(created.uid())
        );
    }

    @Test
    void selectsFilteredSortedItems() throws Exception {
        this.ctx.resultQuery(
            """
            INSERT INTO public.federal_curriculum_requirement (id, federal_curriculum_id, grade,
            subject_id, hours, part_type, is_deleted)
            VALUES (?, ?, ?, (SELECT id FROM subject WHERE name = ?
            AND NOT is_deleted), ?, CAST(? AS curriculum_part_type), ?), (?,
            ?, ?, (SELECT id FROM subject WHERE name = ?
            AND NOT is_deleted), ?, CAST(? AS
            curriculum_part_type), ?), (?, ?, ?, (SELECT id FROM subject WHERE name = ?
            AND NOT is_deleted), ?, CAST(? AS curriculum_part_type), ?)
            """,
            31L,
            1L,
            7,
            "Algebra",
            3,
            "MANDATORY",
            false,
            32L,
            1L,
            7,
            "Geometry",
            2,
            "MANDATORY",
            false,
            33L,
            1L,
            8,
            "Geometry",
            2,
            "MANDATORY",
            false
        ).execute();
        MatcherAssert.assertThat(
            "Selection must apply filter, sorting and first-page limit",
            StreamSupport.stream(
                new RqsPostgres(this.ctx, 1L).selection(
                    new FiltersRequest(
                        new LinkedHashSet<>(
                            List.of(new FilterRequest("grade", "7"))
                        )
                    ),
                    new PageRequest(1, 1),
                    new SortsRequest(
                        List.of(
                            new SortRequest(
                                "subject", Sort.Direction.DESC
                            )
                        )
                    )
                ).iterate().spliterator(),
                false
            ).map(Requirement::uid).toList(),
            Matchers.contains(32L)
        );
    }

    @Test
    void reportsSelectedPage() throws Exception {
        this.ctx.resultQuery(
            """
            INSERT INTO public.federal_curriculum_requirement (id, federal_curriculum_id, grade,
            subject_id, hours, part_type, is_deleted)
            VALUES (?, ?, ?, (SELECT id FROM subject WHERE name = ?
            AND NOT is_deleted), ?, CAST(? AS curriculum_part_type), ?), (?,
            ?, ?, (SELECT id FROM subject WHERE name = ?
            AND NOT is_deleted), ?, CAST(? AS
            curriculum_part_type), ?), (?, ?, ?, (SELECT id FROM subject WHERE name = ?
            AND NOT is_deleted), ?, CAST(? AS curriculum_part_type), ?)
            """,
            31L,
            1L,
            7,
            "Algebra",
            3,
            "MANDATORY",
            false,
            32L,
            1L,
            7,
            "Geometry",
            2,
            "MANDATORY",
            false,
            33L,
            1L,
            8,
            "Geometry",
            2,
            "MANDATORY",
            false
        ).execute();
        MatcherAssert.assertThat(
            "Selection must report pagination for all matching rows",
            new RqsPostgres(this.ctx, 1L).selection(
                new FiltersRequest(
                    new LinkedHashSet<>(
                        List.of(new FilterRequest("grade", "7"))
                    )
                ),
                new PageRequest(1, 1),
                new SortsRequest(
                    List.of(
                        new SortRequest("subject", Sort.Direction.DESC)
                    )
                )
            ).print(new MdJson()).print().path("page"),
            Matchers.is(
                JsonNodeFactory.instance.objectNode()
                    .put("limit", 1)
                    .put("offset", 1)
                    .put("total", 2)
                    .put("totalPages", 2)
                    .put("hasNext", true)
                    .put("hasPrev", false)
            )
        );
    }

    @Test
    void iteratesOnlyActiveRequirementsFromCurrentCurriculum()
        throws Exception {
        final Requirement active = new RqsPostgres(this.ctx, 1L).create(
            new RqInputPart(
                new RqInputHours(
                    new RqInputSubject(
                        new RqInputGrade(new RqInput(), "9"),
                        "508"
                    ),
                    "2"
                ),
                "MANDATORY"
            )
        );
        new RqsPostgres(this.ctx, 1L).remove(
            new RqsPostgres(this.ctx, 1L).create(
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
            ).uid()
        );
        new RqsPostgres(this.ctx, 2L).create(
            new RqInputPart(
                new RqInputHours(
                    new RqInputSubject(
                        new RqInputGrade(new RqInput(), "9"),
                        "512"
                    ),
                    "2"
                ),
                "MANDATORY"
            )
        );
        MatcherAssert.assertThat(
            "Iteration must exclude deleted and foreign requirements",
            StreamSupport.stream(
                new RqsPostgres(this.ctx, 1L).iterate().spliterator(),
                false
            ).map(Requirement::uid).toList(),
            Matchers.contains(active.uid())
        );
    }

    @Test
    void printsRequirementsAsXml() throws Exception {
        new RqsPostgres(this.ctx, 1L).create(
            new RqInputPart(
                new RqInputHours(
                    new RqInputSubject(
                        new RqInputGrade(new RqInput(), "5"),
                        "509"
                    ),
                    "3"
                ),
                "MANDATORY"
            )
        );
        MatcherAssert.assertThat(
            "Requirements collection must print named XML elements",
            new RqsPostgres(this.ctx, 1L)
                .print(new MdXml("requirements")).print(),
            Matchers.allOf(
                Matchers.startsWith("<requirements><requirement>"),
                Matchers.containsString(
                    "<subject><id>509</id><name>Informatics</name><origin>FEDERAL</origin></subject>"
                ),
                Matchers.endsWith("</requirement></requirements>")
            )
        );
    }

    @Test
    void removesRequirement() throws Exception {
        final Requirement created = new RqsPostgres(this.ctx, 1L).create(
            new RqInputPart(
                new RqInputHours(
                    new RqInputSubject(
                        new RqInputGrade(new RqInput(), "5"),
                        "510"
                    ),
                    "1"
                ),
                "MANDATORY"
            )
        );
        new RqsPostgres(this.ctx, 1L).remove(created.uid());
        MatcherAssert.assertThat(
            "Removal must mark the requirement as deleted",
            this.ctx.resultQuery(
                """
                    SELECT federal_curriculum_requirement.is_deleted
                    FROM public.federal_curriculum_requirement
                    WHERE federal_curriculum_requirement.id = ?
                    """,
                created.uid()
            ).fetchOneInto(Boolean.class),
            Matchers.is(true)
        );
    }

    @Test
    void rejectsRemovingMissingRequirement() {
        Assertions.assertThrows(
            RequirementNotFoundException.class,
            () -> new RqsPostgres(this.ctx, 1L).remove(404L)
        );
    }

    @Test
    void rejectsRemovingRequirementTwice() throws Exception {
        final Requirement created = new RqsPostgres(this.ctx, 1L).create(
            new RqInputPart(
                new RqInputHours(
                    new RqInputSubject(
                        new RqInputGrade(new RqInput(), "5"),
                        "506"
                    ),
                    "1"
                ),
                "MANDATORY"
            )
        );
        new RqsPostgres(this.ctx, 1L).remove(created.uid());
        Assertions.assertThrows(
            RequirementNotFoundException.class,
            () -> new RqsPostgres(this.ctx, 1L).remove(created.uid())
        );
    }

    @Test
    void checksHoursAgainstTheOptionalRequirement() throws Exception {
        new RqsPostgres(this.ctx, 1L).create(
            new RqInputHours(
                new RqInputGrade(new RqInputPart(new RqInput(), "OPTIONAL"), "5"), "3"
            )
        );
        Assertions.assertDoesNotThrow(
            () -> new RqsPostgres(this.ctx, 1L).check(5, 3),
            "The explicit grade maximum rejected its boundary"
        );
    }

    @Test
    void rejectsHoursAboveTheOptionalRequirement() throws Exception {
        new RqsPostgres(this.ctx, 1L).create(
            new RqInputHours(
                new RqInputGrade(new RqInputPart(new RqInput(), "OPTIONAL"), "5"), "3"
            )
        );
        Assertions.assertThrows(
            RequirementInputException.class,
            () -> new RqsPostgres(this.ctx, 1L).check(5, 4),
            "Additional hours exceeded the grade maximum"
        );
    }

    @Test
    void distinguishesAnUnspecifiedMaximumFromZero() {
        Assertions.assertThrows(
            RequirementInputException.class,
            () -> new RqsPostgres(this.ctx, 1L).check(5, 0),
            "An absent optional requirement was treated as an explicit zero maximum"
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
            RqsPostgresTest.POSTGRES::getJdbcUrl
        );
        registry.add(
            "spring.datasource.username",
            RqsPostgresTest.POSTGRES::getUsername
        );
        registry.add(
            "spring.datasource.password",
            RqsPostgresTest.POSTGRES::getPassword
        );
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
