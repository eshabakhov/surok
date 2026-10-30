/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

import com.eshabakhov.surok.federal.subject.input.SbInput;
import com.eshabakhov.surok.federal.subject.input.SbInputName;
import java.util.List;
import java.util.stream.StreamSupport;
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
 * Federal subjects backed by the application migrations.
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
final class SbsFederalTest {

    /** Isolated subject catalogue. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:14-alpine");

    @Test
    void listsOnlyActiveFederalSubjects(@Autowired final DSLContext ctx) {
        MatcherAssert.assertThat(
            "The catalogue leaked local or archived subjects",
            StreamSupport.stream(new SbsFederal(ctx).iterate().spliterator(), false)
                .map(Subject::uid).toList(),
            Matchers.contains(29L)
        );
    }

    @Test
    void createsASubjectAvailableToBothSchools(@Autowired final DSLContext ctx) throws Exception {
        final Subject created = new SbsFederal(ctx).create(
            new SbInputName(new SbInput(), "Физика")
        );
        MatcherAssert.assertThat(
            "The federal subject was not shared by both schools",
            List.of(
                new SbsPostgres(ctx, 1L).subject(created.uid()).origin(),
                new SbsPostgres(ctx, 2L).subject(created.uid()).origin()
            ),
            Matchers.contains(Subject.Origin.FEDERAL, Subject.Origin.FEDERAL)
        );
    }

    @Test
    void renamesTheCommonSubjectForEverySchool(@Autowired final DSLContext ctx) throws Exception {
        new SbsFederal(ctx).revise(29L, new SbInputName(new SbInput(), "Алгебра"));
        MatcherAssert.assertThat(
            "Renaming the catalogue entry did not update both schools",
            List.of(
                new SbsPostgres(ctx, 1L).subject(29L).name(),
                new SbsPostgres(ctx, 2L).subject(29L).name()
            ),
            Matchers.contains("Алгебра", "Алгебра")
        );
    }

    @Test
    void rejectsDuplicateFederalNames(@Autowired final DSLContext ctx) {
        Assertions.assertThrows(
            SubjectAlreadyExistsException.class,
            () -> new SbsFederal(ctx).create(new SbInputName(new SbInput(), "Математика")),
            "The catalogue accepted a duplicate federal name"
        );
    }

    @Test
    void cannotRemoveASchoolSubject(@Autowired final DSLContext ctx) {
        Assertions.assertThrows(
            SubjectNotFoundException.class,
            () -> new SbsFederal(ctx).remove(17L),
            "The federal catalogue removed a school's additional subject"
        );
    }

    @Test
    void archivesTheCommonSubject(@Autowired final DSLContext ctx) throws Exception {
        new SbsFederal(ctx).remove(29L);
        MatcherAssert.assertThat(
            "The archived subject remained in the federal catalogue",
            new SbsFederal(ctx).iterate(),
            Matchers.emptyIterable()
        );
    }

    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.liquibase.enabled", () -> true);
        registry.add("spring.datasource.url", SbsFederalTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", SbsFederalTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", SbsFederalTest.POSTGRES::getPassword);
    }
}
