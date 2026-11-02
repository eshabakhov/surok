/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

import com.eshabakhov.surok.federal.subject.input.SbInput;
import com.eshabakhov.surok.federal.subject.input.SbInputName;
import com.eshabakhov.surok.filter.FiltersRequest;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.page.PageRequest;
import com.eshabakhov.surok.sort.SortsRequest;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
 * School subjects backed by the application migrations.
 * For example, the shared catalogue must not expose another school's additional subjects.
 *
 * @since 0.0.1
 */
@Testcontainers
@DirtiesContext
@SpringBootTest
@Sql(
    statements = {
        "TRUNCATE TABLE school, subject RESTART IDENTITY CASCADE",
        "INSERT INTO school (id, name) VALUES (1, 'Лицей'), (2, 'Гимназия')",
        """
        INSERT INTO subject (id, school_id, name)
        VALUES (17, 1, 'Робототехника'), (19, 2, 'Краеведение');
        """,
        """
        INSERT INTO subject (id, origin, name, is_deleted)
        VALUES (29, 'FEDERAL', 'Математика', false), (31, 'FEDERAL', 'Архив', true);
        """
    }
)
final class SbsPostgresTest {

    /** Isolated subject catalogue. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:14-alpine");

    @Test
    void combinesFederalAndOwnAdditionalSubjects(@Autowired final DSLContext ctx) {
        MatcherAssert.assertThat(
            "The school omitted federal subjects or exposed a foreign subject",
            StreamSupport.stream(new SbsPostgres(ctx, 1L).iterate().spliterator(), false)
                .map(Subject::uid).toList(),
            Matchers.containsInAnyOrder(17L, 29L)
        );
    }

    @Test
    void createsOnlyAnAdditionalSubject(@Autowired final DSLContext ctx) throws Exception {
        MatcherAssert.assertThat(
            "Creating a school subject promoted it into the federal catalogue",
            new SbsPostgres(ctx, 1L).create(
                new SbInputName(new SbInput(), "Шахматы")
            ).origin(),
            Matchers.is(Subject.Origin.SCHOOL)
        );
    }

    @Test
    void cannotFindAnotherSchoolsSubject(@Autowired final DSLContext ctx) {
        Assertions.assertThrows(
            SubjectNotFoundException.class,
            () -> new SbsPostgres(ctx, 1L).subject(19L),
            "The school obtained another school's additional subject"
        );
    }

    @Test
    void cannotRenameAFederalSubject(@Autowired final DSLContext ctx) {
        Assertions.assertThrows(
            SubjectReadOnlyException.class,
            () -> new SbsPostgres(ctx, 1L).subject(29L)
                .revise(new SbInputName(new SbInput(), "Новое название")),
            "The school renamed a shared federal subject"
        );
    }

    @Test
    void cannotRemoveAFederalSubject(@Autowired final DSLContext ctx) {
        Assertions.assertThrows(
            SubjectReadOnlyException.class,
            () -> new SbsPostgres(ctx, 1L).remove(29L),
            "The school archived a shared federal subject"
        );
    }

    @Test
    void renamesItsOwnAdditionalSubject(@Autowired final DSLContext ctx) throws Exception {
        MatcherAssert.assertThat(
            "The school could not rename its own additional subject",
            new SbsPostgres(ctx, 1L).revise(
                17L, new SbInputName(new SbInput(), "Технологии")
            ).name(),
            Matchers.is("Технологии")
        );
    }

    @Test
    void countsSharedSubjectsInTheSelectedPage(@Autowired final DSLContext ctx) throws Exception {
        MatcherAssert.assertThat(
            "Pagination omitted shared subjects or counted foreign subjects",
            new SbsPostgres(ctx, 1L).selection(
                new FiltersRequest(Set.of()), new PageRequest(1, 1), new SortsRequest(List.of())
            ).print(new MdJson()).print().path("page").path("total").asInt(),
            Matchers.is(2)
        );
    }

    @Test
    void protectsSharedSubjectsReturnedByIteration(@Autowired final DSLContext ctx) {
        Assertions.assertThrows(
            SubjectReadOnlyException.class,
            () -> StreamSupport.stream(new SbsPostgres(ctx, 1L).iterate().spliterator(), false)
                .filter(subject -> subject.uid().equals(29L)).toList().getFirst()
                .revise(new SbInputName(new SbInput(), "Подмена")),
            "An iterated federal subject escaped school write protection"
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"'FEDERAL', 1", "'SCHOOL', NULL", "'UNKNOWN', 1"})
    void rejectsInconsistentStoredOwnership(final String values, @Autowired final DSLContext ctx) {
        Assertions.assertThrows(
            DataIntegrityViolationException.class,
            () -> ctx.execute(
                String.format(
                    "INSERT INTO subject (origin, school_id, name) VALUES (%s, 'Ошибка')", values
                )
            ),
            "The database accepted an inconsistent subject owner"
        );
    }

    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.liquibase.enabled", () -> true);
        registry.add("spring.datasource.url", SbsPostgresTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", SbsPostgresTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", SbsPostgresTest.POSTGRES::getPassword);
    }
}
