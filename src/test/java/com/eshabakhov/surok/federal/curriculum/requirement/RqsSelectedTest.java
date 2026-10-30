/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement;

import com.eshabakhov.surok.Filter;
import com.eshabakhov.surok.Sort;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInput;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInputGrade;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInputHours;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInputPart;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInputSubject;
import com.eshabakhov.surok.filter.FiltersRequest;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.page.PageRequest;
import com.eshabakhov.surok.page.ResponsePage;
import com.eshabakhov.surok.sort.SortsRequest;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
 * Test for {@link RqsSelected}.
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
final class RqsSelectedTest {

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
    RqsSelectedTest(final DSLContext ctx) {
        this.ctx = ctx;
    }

    @Test
    void delegatesCreation() throws Exception {
        MatcherAssert.assertThat(
            "Selected requirements must delegate creation",
            new RqsSelected(
                this.prepare(0L),
                List.of(),
                new ResponsePage(new PageRequest(15, 1), 0)
            ).create(this.input("5")).uid(),
            Matchers.is(1L)
        );
    }

    @Test
    void delegatesLookup() throws Exception {
        MatcherAssert.assertThat(
            "Selected requirements must delegate lookup",
            new RqsSelected(
                this.prepare(5L),
                List.of(),
                new ResponsePage(new PageRequest(15, 1), 0)
            ).requirement(5L).uid(),
            Matchers.is(5L)
        );
    }

    @Test
    void delegatesSelection() throws Exception {
        MatcherAssert.assertThat(
            "Selected requirements must delegate another selection",
            new RqsSelected(
                this.prepare(6L),
                List.of(),
                new ResponsePage(new PageRequest(15, 1), 0)
            ).selection(
                new FiltersRequest(Set.<Filter>of()),
                new PageRequest(10, 1),
                new SortsRequest(List.<Sort>of())
            ).iterate().iterator().next().uid(),
            Matchers.is(6L)
        );
    }

    @Test
    void exposesSelectedItems() {
        final Iterable<Requirement> items = List.of(
            new RqMandatory(this.ctx, 1L)
        );
        MatcherAssert.assertThat(
            "Selected requirements must expose their items",
            new RqsSelected(
                this.prepare(0L),
                items,
                new ResponsePage(new PageRequest(15, 1), 1)
            ).iterate(),
            Matchers.sameInstance(items)
        );
    }

    @Test
    void printsItemsAndPage() throws Exception {
        MatcherAssert.assertThat(
            "Selected requirements must print items and pagination",
            new RqsSelected(
                this.prepare(3L),
                List.of(new RqV1(new RqMandatory(this.ctx, 3L))),
                new ResponsePage(new PageRequest(10, 2), 25)
            ).print(new MdJson()).print(),
            Matchers.is(
                JsonNodeFactory.instance.objectNode().<ObjectNode>set(
                    "items",
                    JsonNodeFactory.instance.arrayNode().add(
                        JsonNodeFactory.instance.objectNode().put("id", 3L)
                            .put("grade", 5).<ObjectNode>set(
                                "subject", JsonNodeFactory.instance.objectNode().put("id", 514L)
                                    .put("name", "Математика").put("origin", "FEDERAL")
                            ).put("hours", 6)
                            .put("partType", "MANDATORY")
                    )
                ).<ObjectNode>set(
                    "page",
                    JsonNodeFactory.instance.objectNode()
                        .put("limit", 10)
                        .put("offset", 2)
                        .put("total", 25)
                        .put("totalPages", 3)
                        .put("hasNext", true)
                        .put("hasPrev", true)
                )
            )
        );
    }

    @Test
    void delegatesRemoval() throws Exception {
        final Requirements origin = this.prepare(9L);
        new RqsSelected(
            origin,
            List.of(),
            new ResponsePage(new PageRequest(15, 1), 0)
        ).remove(9L);
        MatcherAssert.assertThat(
            "Selected removal must remove the persisted requirement",
            origin.iterate(),
            Matchers.emptyIterable()
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
        registry.add("spring.datasource.url", RqsSelectedTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", RqsSelectedTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", RqsSelectedTest.POSTGRES::getPassword);
    }
}
