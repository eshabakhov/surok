/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement;

import com.eshabakhov.surok.Filter;
import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.Sort;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInput;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInputGrade;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInputHours;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInputPart;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInputSubject;
import com.eshabakhov.surok.filter.FilterRequest;
import com.eshabakhov.surok.filter.FiltersRequest;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.page.PageRequest;
import com.eshabakhov.surok.sort.SortRequest;
import com.eshabakhov.surok.sort.SortsRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
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
 * Test for {@link RqsWeb}.
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
final class RqsWebTest {

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
    RqsWebTest(final DSLContext ctx) {
        this.ctx = ctx;
    }

    @Test
    void delegatesCreation() throws Exception {
        MatcherAssert.assertThat(
            "Web requirements must delegate creation",
            new RqsWeb(
                this.prepare(0L),
                new FiltersRequest(Set.of()),
                new SortsRequest(List.of())
            ).create(this.input("5")).uid(),
            Matchers.is(1L)
        );
    }

    @Test
    void delegatesLookup() throws Exception {
        MatcherAssert.assertThat(
            "Web requirements must delegate lookup",
            new RqsWeb(
                this.prepare(4L),
                new FiltersRequest(Set.of()),
                new SortsRequest(List.of())
            ).requirement(4L).uid(),
            Matchers.is(4L)
        );
    }

    @Test
    void delegatesSelection() throws Exception {
        final Filters filters = new FiltersRequest(Set.<Filter>of());
        final Page page = new PageRequest(5, 1);
        final Sorts sorts = new SortsRequest(List.<Sort>of());
        MatcherAssert.assertThat(
            "Web requirements must delegate selection",
            new RqsWeb(
                this.prepare(3L),
                filters,
                sorts
            ).selection(filters, page, sorts).iterate()
                .iterator().next().uid(),
            Matchers.is(3L)
        );
    }

    @Test
    void delegatesIteration() {
        MatcherAssert.assertThat(
            "Web requirements must delegate iteration",
            new RqsWeb(
                this.prepare(2L),
                new FiltersRequest(Set.of()),
                new SortsRequest(List.of())
            ).iterate().iterator().next().uid(),
            Matchers.is(2L)
        );
    }

    @Test
    void delegatesRemoval() throws Exception {
        final Requirements origin = this.prepare(8L);
        new RqsWeb(
            origin,
            new FiltersRequest(Set.of()),
            new SortsRequest(List.of())
        ).remove(8L);
        MatcherAssert.assertThat(
            "Web removal must remove the persisted requirement",
            origin.iterate(),
            Matchers.emptyIterable()
        );
    }

    @Test
    void printsFiltersAndSorts() throws Exception {
        MatcherAssert.assertThat(
            "Web requirements must append filter and sort state",
            new RqsWeb(
                this.prepare(0L),
                new FiltersRequest(
                    Set.of(new FilterRequest("grade", "7"))
                ),
                new SortsRequest(
                    List.of(
                        new SortRequest("subject", Sort.Direction.DESC)
                    )
                )
            ).print(new MdJson()).print(),
            Matchers.is(
                new ObjectMapper().readTree(
                    """
                        {
                            "items": [],
                            "filters": [{"name": "grade", "value": "7"}],
                            "sorts": [{"name": "subject", "direction": "DESC"}]
                        }
                    """
                )
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
        registry.add("spring.datasource.url", RqsWebTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", RqsWebTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", RqsWebTest.POSTGRES::getPassword);
    }
}
