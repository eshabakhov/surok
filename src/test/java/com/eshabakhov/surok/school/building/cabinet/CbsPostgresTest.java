/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.building.cabinet;

import com.eshabakhov.surok.Sort;
import com.eshabakhov.surok.filter.FilterRequest;
import com.eshabakhov.surok.filter.FiltersRequest;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.media.MdXml;
import com.eshabakhov.surok.page.PageRequest;
import com.eshabakhov.surok.school.building.cabinet.input.CabinetInputException;
import com.eshabakhov.surok.school.building.cabinet.input.CbInput;
import com.eshabakhov.surok.school.building.cabinet.input.CbInputName;
import com.eshabakhov.surok.sort.SortRequest;
import com.eshabakhov.surok.sort.SortsRequest;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Cabinet persistence, selection and building boundaries.
 *
 * @since 0.0.1
 * @checkstyle ProhibitFieldsInTestClassesCheck (1000 lines)
 */
@Testcontainers
@DirtiesContext
@SpringBootTest
@SuppressWarnings({
    "PMD.UnusedPrivateMethod", "UnusedMethod", "PMD.UnitTestContainsTooManyAsserts"
})
final class CbsPostgresTest {

    /** Postgres container. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("surok")
            .withUsername("test")
            .withPassword("test");

    /** Database context. */
    private final DSLContext ctx;

    @Autowired
    CbsPostgresTest(final DSLContext ctx) {
        this.ctx = ctx;
    }

    @BeforeEach
    void insertBuildingCabinets() {
        this.ctx.resultQuery(
            """
                INSERT INTO public.school (id, name)
                VALUES (1, 'First school'), (2, 'Second school')
                """
        ).execute();
        this.ctx.resultQuery(
            """
                INSERT INTO public.building (id, school_id, name)
                VALUES (1, 1, 'North building'), (2, 1, 'South building'),
                    (3, 2, 'Foreign building')
                """
        ).execute();
        this.ctx.resultQuery(
            """
                INSERT INTO public.cabinet (id, building_id, name, is_deleted)
                VALUES (10, 1, 'North', false), (11, 1, 'South', false),
                    (12, 1, 'North annex', false), (13, 1, 'North removed', true),
                    (20, 2, 'North', false), (30, 3, 'North foreign', false)
                """
        ).execute();
    }

    @AfterEach
    void truncateSchools() {
        this.ctx.resultQuery(
            """
                TRUNCATE TABLE public.school, subject RESTART IDENTITY CASCADE
                """
        ).execute();
    }

    @Test
    void createsAndPrintsCabinet() throws Exception {
        final Cabinet created = new CbsPostgres(this.ctx, 1L)
            .create(new CbInputName(new CbInput(), " East "));
        MatcherAssert.assertThat(
            "Created cabinet must be readable from its building",
            new CbsPostgres(this.ctx, 1L).cabinet(created.uid())
                .print(new MdJson()).print().get("name").asText(),
            Matchers.is("East")
        );
        MatcherAssert.assertThat(
            "The same object must support XML output",
            created.print(new MdXml("cabinet")).print(),
            Matchers.containsString("<name>East</name>")
        );
    }

    @Test
    void rejectsDuplicateWithinBuilding() {
        MatcherAssert.assertThat(
            "Transaction rollback must retain the domain conflict",
            Assertions.assertThrows(
                DataAccessException.class,
                () -> new CbsPostgres(this.ctx, 1L)
                    .create(new CbInputName(new CbInput(), "North"))
            ).getCause(),
            Matchers.instanceOf(CabinetAlreadyExistsException.class)
        );
    }

    @Test
    void allowsSameNameInAnotherBuilding() throws Exception {
        MatcherAssert.assertThat(
            "Names are unique within a building",
            new CbsPostgres(this.ctx, 2L)
                .create(new CbInputName(new CbInput(), "South"))
                .print(new MdJson()).print().get("name").asText(),
            Matchers.is("South")
        );
    }

    @ParameterizedTest
    @ValueSource(longs = {13L, 20L, 30L, 999L})
    void rejectsMissingDeletedAndForeignCabinets(final long id) {
        Assertions.assertThrows(
            CabinetNotFoundException.class,
            () -> new CbsPostgres(this.ctx, 1L).cabinet(id)
        );
    }

    @Test
    void revisesThroughCollection() throws Exception {
        new CbsPostgres(this.ctx, 1L).revise(
            10L, new CbInputName(new CbInput(), "Renamed")
        );
        MatcherAssert.assertThat(
            "Revision must persist the name without replacing the identity",
            new CbPostgres(this.ctx, 10L).print(new MdJson()).print().get("name").asText(),
            Matchers.is("Renamed")
        );
    }

    @Test
    void rejectsInvalidRevisionWithoutWriting() throws Exception {
        final Cabinets cabinets = new CbsPostgres(this.ctx, 1L);
        Assertions.assertThrows(
            CabinetInputException.class,
            () -> cabinets.revise(10L, new CbInputName(new CbInput(), " "))
        );
        MatcherAssert.assertThat(
            "Rejected input must not change stored data",
            cabinets.cabinet(10L).print(new MdJson()).print().get("name").asText(),
            Matchers.is("North")
        );
    }

    @Test
    void rejectsRevisionFromAnotherBuilding() {
        Assertions.assertThrows(
            CabinetNotFoundException.class,
            () -> new CbsPostgres(this.ctx, 1L)
                .revise(20L, new CbInputName(new CbInput(), "Foreign"))
        );
    }

    @Test
    void removesSoftlyAndAllowsRecreation() throws Exception {
        final Cabinets cabinets = new CbsPostgres(this.ctx, 1L);
        cabinets.remove(10L);
        MatcherAssert.assertThat(
            "Removal must preserve the row and mark it deleted",
            this.ctx.resultQuery(
                """
                    SELECT is_deleted FROM public.cabinet WHERE id = ?
                    """,
                10L
            ).fetchOne(0, Boolean.class),
            Matchers.is(true)
        );
        MatcherAssert.assertThat(
            "A deleted name may be reused",
            cabinets.create(new CbInputName(new CbInput(), "North")).uid(),
            Matchers.not(10L)
        );
    }

    @ParameterizedTest
    @ValueSource(longs = {13L, 20L, 30L, 999L})
    void rejectsRemovalOutsideActiveBuildingCabinets(final long id) {
        Assertions.assertThrows(
            CabinetNotFoundException.class,
            () -> new CbsPostgres(this.ctx, 1L).remove(id)
        );
    }

    @Test
    void preventsUseOfDeletedObject() throws Exception {
        final Cabinets cabinets = new CbsPostgres(this.ctx, 1L);
        final Cabinet cabinet = cabinets.cabinet(10L);
        cabinets.remove(10L);
        Assertions.assertThrows(
            CabinetNotFoundException.class,
            () -> cabinet.revise(new CbInputName(new CbInput(), "Restored"))
        );
        Assertions.assertThrows(
            CabinetNotFoundException.class, () -> cabinet.print(new MdJson())
        );
    }

    @Test
    void iteratesOnlyActiveBuildingCabinets() {
        MatcherAssert.assertThat(
            "Iteration must exclude deleted and foreign cabinets",
            StreamSupport.stream(new CbsPostgres(this.ctx, 1L).iterate().spliterator(), false)
                .map(Cabinet::uid).toList(),
            Matchers.contains(10L, 12L, 11L)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"name", "name_ct"})
    void filtersAndPaginatesWithinBuilding(final String filter) throws Exception {
        final JsonNode printed = new CbsPostgres(this.ctx, 1L).selection(
            new FiltersRequest(Set.of(new FilterRequest(filter, " NORTH "))),
            new PageRequest(1, 2), new SortsRequest(List.of())
        ).print(new MdJson()).print();
        MatcherAssert.assertThat(
            "Second page must contain the second matching active cabinet",
            printed.get("items").get(0).get("id").asLong(), Matchers.is(12L)
        );
        MatcherAssert.assertThat(
            "Count must use the same building, deletion and search restrictions",
            printed.get("page").get("total").asInt(), Matchers.is(2)
        );
    }

    @Test
    void sortsRequestedNamesDescending() throws Exception {
        MatcherAssert.assertThat(
            "Requested ordering must be applied before pagination",
            new CbsPostgres(this.ctx, 1L).selection(
                new FiltersRequest(Set.of()), new PageRequest(1, 1),
                new SortsRequest(List.of(new SortRequest("name", Sort.Direction.DESC)))
            ).print(new MdJson()).print().get("items").get(0).get("id").asLong(),
            Matchers.is(11L)
        );
    }

    @Test
    void printsEmptyPageWithTotal() throws Exception {
        final JsonNode printed = new CbsPostgres(this.ctx, 1L).selection(
            new FiltersRequest(Set.of()), new PageRequest(2, 3), new SortsRequest(List.of())
        ).print(new MdJson()).print();
        MatcherAssert.assertThat(
            "An out-of-range page must contain an empty collection",
            printed.get("items").isEmpty(), Matchers.is(true)
        );
        MatcherAssert.assertThat(
            "An empty page must retain the total count",
            printed.get("page").get("total").asInt(), Matchers.is(3)
        );
    }

    @Test
    void delegatesSelectedCollectionOperations() throws Exception {
        final Cabinets selected = new CbsPostgres(this.ctx, 1L).selection(
            new FiltersRequest(Set.of()), new PageRequest(1, 1), new SortsRequest(List.of())
        );
        final Cabinet created = selected.create(new CbInputName(new CbInput(), "East"));
        selected.revise(created.uid(), new CbInputName(new CbInput(), "West"));
        MatcherAssert.assertThat(
            "Operations on a selection must still address the building's collection",
            selected.cabinet(created.uid()).print(new MdJson()).print().get("name").asText(),
            Matchers.is("West")
        );
        selected.remove(created.uid());
        Assertions.assertThrows(
            CabinetNotFoundException.class, () -> selected.cabinet(created.uid())
        );
    }

    @DynamicPropertySource
    private static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.liquibase.enabled", () -> true);
        registry.add("spring.datasource.url", CbsPostgresTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", CbsPostgresTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", CbsPostgresTest.POSTGRES::getPassword);
    }
}
