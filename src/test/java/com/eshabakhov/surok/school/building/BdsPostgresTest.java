/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.building;

import com.eshabakhov.surok.Sort;
import com.eshabakhov.surok.filter.FilterRequest;
import com.eshabakhov.surok.filter.FiltersRequest;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.media.MdXml;
import com.eshabakhov.surok.page.PageRequest;
import com.eshabakhov.surok.school.building.cabinet.Cabinet;
import com.eshabakhov.surok.school.building.cabinet.CabinetNotFoundException;
import com.eshabakhov.surok.school.building.cabinet.input.CbInput;
import com.eshabakhov.surok.school.building.cabinet.input.CbInputName;
import com.eshabakhov.surok.school.building.input.BdInput;
import com.eshabakhov.surok.school.building.input.BdInputName;
import com.eshabakhov.surok.school.building.input.BuildingInputException;
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
 * Building persistence, selection and school boundaries.
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
final class BdsPostgresTest {

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
    BdsPostgresTest(final DSLContext ctx) {
        this.ctx = ctx;
    }

    @BeforeEach
    void insertSchoolBuildings() {
        this.ctx.resultQuery(
            """
                INSERT INTO public.school (id, name)
                VALUES (1, 'First school'), (2, 'Second school')
                """
        ).execute();
        this.ctx.resultQuery(
            """
                INSERT INTO public.building (id, school_id, name, is_deleted)
                VALUES (10, 1, 'North', false), (11, 1, 'South', false),
                    (12, 1, 'North annex', false), (13, 1, 'North removed', true),
                    (20, 2, 'North', false)
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
    void createsAndPrintsBuilding() throws Exception {
        final Building created = new BdsPostgres(this.ctx, 1L)
            .create(new BdInputName(new BdInput(), " East "));
        MatcherAssert.assertThat(
            "Created building must be readable from its school",
            new BdsPostgres(this.ctx, 1L).building(created.uid())
                .print(new MdJson()).print().get("name").asText(),
            Matchers.is("East")
        );
        MatcherAssert.assertThat(
            "The same object must support XML output",
            created.print(new MdXml("building")).print(),
            Matchers.containsString("<name>East</name>")
        );
    }

    @Test
    void rejectsDuplicateWithinSchool() {
        MatcherAssert.assertThat(
            "Transaction rollback must retain the domain conflict",
            Assertions.assertThrows(
                DataAccessException.class,
                () -> new BdsPostgres(this.ctx, 1L)
                    .create(new BdInputName(new BdInput(), "North"))
            ).getCause(),
            Matchers.instanceOf(BuildingAlreadyExistsException.class)
        );
    }

    @Test
    void allowsSameNameInAnotherSchool() throws Exception {
        MatcherAssert.assertThat(
            "Names are unique within a school",
            new BdsPostgres(this.ctx, 2L)
                .create(new BdInputName(new BdInput(), "South"))
                .print(new MdJson()).print().get("name").asText(),
            Matchers.is("South")
        );
    }

    @ParameterizedTest
    @ValueSource(longs = {13L, 20L, 999L})
    void rejectsMissingDeletedAndForeignBuildings(final long id) {
        Assertions.assertThrows(
            BuildingNotFoundException.class,
            () -> new BdsPostgres(this.ctx, 1L).building(id)
        );
    }

    @Test
    void revisesThroughCollection() throws Exception {
        new BdsPostgres(this.ctx, 1L).revise(
            10L, new BdInputName(new BdInput(), "Renamed")
        );
        MatcherAssert.assertThat(
            "Revision must persist the name without replacing the identity",
            new BdsPostgres(this.ctx, 1L).building(10L)
                .print(new MdJson()).print().get("name").asText(),
            Matchers.is("Renamed")
        );
    }

    @Test
    void rejectsInvalidRevisionWithoutWriting() throws Exception {
        final Buildings buildings = new BdsPostgres(this.ctx, 1L);
        Assertions.assertThrows(
            BuildingInputException.class,
            () -> buildings.revise(10L, new BdInputName(new BdInput(), " "))
        );
        MatcherAssert.assertThat(
            "Rejected input must not change stored data",
            buildings.building(10L).print(new MdJson()).print().get("name").asText(),
            Matchers.is("North")
        );
    }

    @Test
    void rejectsRevisionFromAnotherSchool() {
        Assertions.assertThrows(
            BuildingNotFoundException.class,
            () -> new BdsPostgres(this.ctx, 1L)
                .revise(20L, new BdInputName(new BdInput(), "Foreign"))
        );
    }

    @Test
    void removesSoftlyAndAllowsRecreation() throws Exception {
        final Buildings buildings = new BdsPostgres(this.ctx, 1L);
        buildings.remove(10L);
        MatcherAssert.assertThat(
            "Removal must preserve the row and mark it deleted",
            this.ctx.resultQuery(
                """
                    SELECT is_deleted FROM public.building WHERE id = ?
                    """,
                10L
            ).fetchOne(0, Boolean.class),
            Matchers.is(true)
        );
        MatcherAssert.assertThat(
            "A deleted name may be reused",
            buildings.create(new BdInputName(new BdInput(), "North")).uid(),
            Matchers.not(10L)
        );
    }

    @ParameterizedTest
    @ValueSource(longs = {13L, 20L, 999L})
    void rejectsRemovalOutsideActiveSchoolBuildings(final long id) {
        Assertions.assertThrows(
            BuildingNotFoundException.class,
            () -> new BdsPostgres(this.ctx, 1L).remove(id)
        );
    }

    @Test
    void preventsUseOfDeletedObject() throws Exception {
        final Buildings buildings = new BdsPostgres(this.ctx, 1L);
        final Building building = buildings.building(10L);
        buildings.remove(10L);
        Assertions.assertThrows(
            BuildingNotFoundException.class,
            () -> building.revise(new BdInputName(new BdInput(), "Restored"))
        );
        Assertions.assertThrows(
            BuildingNotFoundException.class, () -> building.print(new MdJson())
        );
    }

    @Test
    void iteratesOnlyActiveSchoolBuildings() {
        MatcherAssert.assertThat(
            "Iteration must exclude deleted and foreign buildings",
            StreamSupport.stream(new BdsPostgres(this.ctx, 1L).iterate().spliterator(), false)
                .map(Building::uid).toList(),
            Matchers.contains(10L, 12L, 11L)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"name", "name_ct"})
    void filtersAndPaginatesWithinSchool(final String filter) throws Exception {
        final JsonNode printed = new BdsPostgres(this.ctx, 1L).selection(
            new FiltersRequest(Set.of(new FilterRequest(filter, " NORTH "))),
            new PageRequest(1, 2), new SortsRequest(List.of())
        ).print(new MdJson()).print();
        MatcherAssert.assertThat(
            "Second page must contain the second matching active building",
            printed.get("items").get(0).get("id").asLong(), Matchers.is(12L)
        );
        MatcherAssert.assertThat(
            "Count must use the same school, deletion and search restrictions",
            printed.get("page").get("total").asInt(), Matchers.is(2)
        );
    }

    @Test
    void sortsRequestedNamesDescending() throws Exception {
        MatcherAssert.assertThat(
            "Requested ordering must be applied before pagination",
            new BdsPostgres(this.ctx, 1L).selection(
                new FiltersRequest(Set.of()), new PageRequest(1, 1),
                new SortsRequest(List.of(new SortRequest("name", Sort.Direction.DESC)))
            ).print(new MdJson()).print().get("items").get(0).get("id").asLong(),
            Matchers.is(11L)
        );
    }

    @Test
    void printsEmptyPageWithTotal() throws Exception {
        final JsonNode printed = new BdsPostgres(this.ctx, 1L).selection(
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
        final Buildings selected = new BdsPostgres(this.ctx, 1L).selection(
            new FiltersRequest(Set.of()), new PageRequest(1, 1), new SortsRequest(List.of())
        );
        final Building created = selected.create(new BdInputName(new BdInput(), "East"));
        selected.revise(created.uid(), new BdInputName(new BdInput(), "West"));
        MatcherAssert.assertThat(
            "Operations on a selection must still address the school's collection",
            selected.building(created.uid()).print(new MdJson()).print().get("name").asText(),
            Matchers.is("West")
        );
        selected.remove(created.uid());
        Assertions.assertThrows(
            BuildingNotFoundException.class, () -> selected.building(created.uid())
        );
    }

    @Test
    void keepsCabinetsScopedToBuilding() throws Exception {
        final Buildings buildings = new BdsPostgres(this.ctx, 1L);
        final Cabinet cabinet = buildings.building(10L).cabinets()
            .create(new CbInputName(new CbInput(), "Room"));
        Assertions.assertThrows(
            CabinetNotFoundException.class,
            () -> buildings.building(11L).cabinets().cabinet(cabinet.uid())
        );
    }

    @DynamicPropertySource
    private static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.liquibase.enabled", () -> true);
        registry.add("spring.datasource.url", BdsPostgresTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", BdsPostgresTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", BdsPostgresTest.POSTGRES::getPassword);
    }
}
