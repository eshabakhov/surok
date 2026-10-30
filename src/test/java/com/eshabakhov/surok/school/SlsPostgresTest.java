/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.Schools;
import com.eshabakhov.surok.Sort;
import com.eshabakhov.surok.filter.FilterRequest;
import com.eshabakhov.surok.filter.FiltersRequest;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.page.PageRequest;
import com.eshabakhov.surok.school.input.SlInput;
import com.eshabakhov.surok.school.input.SlInputName;
import com.eshabakhov.surok.sort.SortRequest;
import com.eshabakhov.surok.sort.SortsRequest;
import java.util.List;
import java.util.Set;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Test for {@link SlsPostgres}.
 *
 * @since 0.0.1
 * @checkstyle NoJavadocForPrivateMethodsCheck (1000 lines)
 * @checkstyle ProhibitFieldsInTestClassesCheck (1000 lines)
 */
@Testcontainers
@DirtiesContext
@SpringBootTest
@SuppressWarnings({"PMD.UnusedPrivateMethod", "UnusedMethod"})
final class SlsPostgresTest {

    /** Postgres container. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("surok")
        .withUsername("test")
        .withPassword("test");

    /** JOOQ DSL context for executing database queries. */
    private final DSLContext ctx;

    @Autowired
    SlsPostgresTest(final DSLContext ctx) {
        this.ctx = ctx;
    }

    /** Checks Unicode values are bound as literal data. */
    @Test
    void preservesQuotedUnicodeSchoolNames() throws Exception {
        final String name = String.format(
            "Школа O'Brien '%s'; DROP TABLE school; --",
            java.util.UUID.randomUUID()
        );
        MatcherAssert.assertThat(
            "School creation changed the bound name",
            this.collection().create(new SlInputName(new SlInput(), name))
            .print(new MdJson()).print().path("name").asText(),
            Matchers.equalTo(name)
        );
    }

    /** Checks identity lookup preserves quoted Unicode names. */
    @Test
    void findsSchoolsByIdentity() throws Exception {
        final Schools schools = this.collection();
        final String name = String.format("Лицей '%s' OR '1'='1", java.util.UUID.randomUUID());
        MatcherAssert.assertThat(
            "School lookup changed its quoted Unicode name",
            schools.school(
                schools.create(new SlInputName(new SlInput(), name)).uid()
            ).print(new MdJson()).print().path("name").asText(),
            Matchers.equalTo(name)
        );
    }

    /** Checks normalization is applied in persistence input. */
    @Test
    void revisesSchoolUsingValidatedInput() throws Exception {
        final Schools schools = this.collection();
        MatcherAssert.assertThat(
            "Revised school name was not normalized",
            schools.revise(
                schools.create(new SlInputName(new SlInput(), "Школа")).uid(),
                new SlInputName(new SlInput(), "  Лицей  ")
            ).print(new MdJson()).print().path("name").asText(),
            Matchers.equalTo("Лицей")
        );
    }

    /** Checks selection excludes soft deleted rows without caller conditions. */
    @Test
    void scopesSchoolSelectionsToActiveRows() throws Exception {
        final Schools schools = this.collection();
        final School removed = schools.create(new SlInputName(new SlInput(), "Удалённая"));
        schools.create(new SlInputName(new SlInput(), "Живая"));
        schools.remove(removed.uid());
        MatcherAssert.assertThat(
            "School selection counted deleted rows",
            schools.selection(
                new FiltersRequest(Set.of()), new PageRequest(1, 1),
                new SortsRequest(List.of())
            ).print(new MdJson()).print().path("page").path("total").asInt(),
            Matchers.equalTo(1)
        );
    }

    /** Checks filtering, descending sorting and pagination share the same scope. */
    @Test
    void filtersAndSortsSchoolPages() throws Exception {
        final Schools schools = this.collection();
        schools.create(new SlInputName(new SlInput(), "Лицей А"));
        schools.create(new SlInputName(new SlInput(), "Лицей Я"));
        schools.create(new SlInputName(new SlInput(), "Школа"));
        MatcherAssert.assertThat(
            "School page ignored its filter or descending order",
            schools.selection(
                new FiltersRequest(Set.of(new FilterRequest("name", "Лицей"))),
                new PageRequest(1, 2), new SortsRequest(
                    List.of(
                        new SortRequest(
                            "name",
                            Sort.Direction.DESC
                        )
                    )
                )
            ).print(new MdJson()).print().path("items").path(0).path("name").asText(),
            Matchers.equalTo("Лицей А")
        );
    }

    /** Checks updates cannot revive a school through a stale object. */
    @Test
    void rejectsRevisionOfDeletedSchools() throws Exception {
        final Schools schools = this.collection();
        final School school = schools.create(new SlInputName(new SlInput(), "Школа"));
        schools.remove(school.uid());
        Assertions.assertThrows(
            SchoolNotFoundException.class,
            () -> school.revise(new SlInputName(new SlInput(), "Лицей")),
            "Deleted school was revised"
        );
    }

    /** Checks iteration consistently hides deleted schools. */
    @Test
    void iteratesOnlyActiveSchools() throws Exception {
        final Schools schools = this.collection();
        schools.remove(schools.create(new SlInputName(new SlInput(), "Удалённая")).uid());
        MatcherAssert.assertThat(
            "School iteration exposed deleted rows", schools.iterate(),
            Matchers.emptyIterable()
        );
    }

    /**
     * Prepares a clean collection before each behavioral scenario.
     *
     * @return Empty school collection
     */
    private Schools collection() {
        this.ctx.execute("TRUNCATE TABLE public.school, subject RESTART IDENTITY CASCADE");
        return new SlsPostgres(this.ctx);
    }

    /**
     * Configures the disposable database.
     *
     * @param registry Test properties
     */
    @DynamicPropertySource
    private static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.datasource.url", SlsPostgresTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", SlsPostgresTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", SlsPostgresTest.POSTGRES::getPassword);
        registry.add("spring.liquibase.enabled", () -> true);
    }
}
