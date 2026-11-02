/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.autofill;

import com.eshabakhov.surok.federal.curriculum.CrPostgres;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.school.schedule.SdPostgres;
import java.util.List;
import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
 * Browser draft decoration with real persistence.
 * For example, saving through the decorator remains visible when reopening the draft.
 *
 * @since 0.0.1
 */
@Testcontainers
@DirtiesContext
@SpringBootTest
@Sql(
    statements = {
        "TRUNCATE TABLE school, subject, federal_curriculum RESTART IDENTITY CASCADE",
        "INSERT INTO school (id, name) VALUES (13, 'Школа №13')",
        "INSERT INTO schedule (id, school_id, name) VALUES (107, 13, 'Учебный год')",
        "INSERT INTO school_class (id, school_id, grade, litera) VALUES (23, 13, 5, 'Б')",
        """
        INSERT INTO subject (id, origin, name)
        VALUES (37, 'FEDERAL', 'Математика');
        """,
        """
        INSERT INTO federal_curriculum
            (id, title, education_level, study_week_type, version, academic_year)
        VALUES (67, 'Первый план', 'BASIC', 'FIVE_DAYS', '1', '2026/2027'),
            (71, 'Второй план', 'BASIC', 'FIVE_DAYS', '2', '2026/2027');
        """,
        """
        INSERT INTO federal_curriculum_requirement
            (federal_curriculum_id, grade, subject_id, hours, part_type)
        VALUES (67, 5, 37, 3, 'MANDATORY');
        """
    }
)
final class LdWebTest {

    /** Isolated PostgreSQL database for application migrations. */
    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:14-alpine");

    @Test
    void savesTheDecoratedDraft(@Autowired final DSLContext ctx) throws Exception {
        new LdWeb(new LdPostgres(ctx, new SdPostgres(ctx, 107L))).save(
            new CrPostgres(ctx, 67L), Map.of()
        );
        MatcherAssert.assertThat(
            "Saving through the decorator lost the selected curriculum",
            new LdPostgres(ctx, new SdPostgres(ctx, 107L)).curriculum().uid(),
            Matchers.is(67L)
        );
    }

    @Test
    void restoresTheDecoratedCurriculum(@Autowired final DSLContext ctx) throws Exception {
        final LoadDraft draft = new LdPostgres(ctx, new SdPostgres(ctx, 107L));
        draft.save(new CrPostgres(ctx, 67L), Map.of());
        MatcherAssert.assertThat(
            "The decorator failed to restore the saved curriculum",
            new LdWeb(draft).curriculum().uid(),
            Matchers.is(67L)
        );
    }

    @Test
    void switchesTheDecoratedCurriculum(@Autowired final DSLContext ctx) throws Exception {
        final LoadDraft draft = new LdPostgres(ctx, new SdPostgres(ctx, 107L));
        draft.save(new CrPostgres(ctx, 67L), Map.of());
        new LdWeb(draft).revise(new CrPostgres(ctx, 71L));
        MatcherAssert.assertThat(
            "Switching through the decorator retained the previous curriculum",
            draft.curriculum().uid(),
            Matchers.is(71L)
        );
    }

    @Test
    void appliesTheDecoratedLoads(@Autowired final DSLContext ctx) throws Exception {
        final LoadDraft draft = new LdPostgres(ctx, new SdPostgres(ctx, 107L));
        draft.save(new CrPostgres(ctx, 67L), Map.of());
        new LdWeb(draft).apply();
        MatcherAssert.assertThat(
            "Applying through the decorator omitted the mandatory teaching load",
            ctx.fetch("SELECT school_class_id, subject_id, hours FROM class_curriculum")
                .map(row -> List.of(row.get(0), row.get(1), row.get(2))),
            Matchers.contains(List.of(23L, 37L, 3))
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void printsTheSavedStateWithoutChangingTheDraft(
        final boolean saved, @Autowired final DSLContext ctx
    ) throws Exception {
        final LoadDraft draft = new LdPostgres(ctx, new SdPostgres(ctx, 107L));
        int count = 0;
        if (saved) {
            draft.save(new CrPostgres(ctx, 67L), Map.of());
            count = 1;
        }
        MdJson expected = new MdJson();
        if (saved) {
            expected = expected.with("draft", draft);
        }
        MatcherAssert.assertThat(
            "The browser representation lost the draft or changed its saved state",
            List.of(
                new LdWeb(draft).print(new MdJson()).print(),
                ctx.fetchSingle("SELECT count(*) FROM load_draft").get(0, Integer.class)
            ),
            Matchers.contains(
                expected.with("saved", saved).print(), count
            )
        );
    }

    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.liquibase.enabled", () -> true);
        registry.add("spring.datasource.url", LdWebTest.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", LdWebTest.POSTGRES::getUsername);
        registry.add("spring.datasource.password", LdWebTest.POSTGRES::getPassword);
    }
}
