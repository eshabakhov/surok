/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schoolclass;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * School class grades read directly from database records.
 * For example, load planning uses the grade without rendering the school class.
 *
 * @since 0.0.1
 */
final class ScPostgresTest {

    @Test
    void readsTheGradeIndependentlyOfTheClassIdentity() throws Exception {
        final DSLContext context = DSL.using(SQLDialect.POSTGRES);
        final Result<Record1<Integer>> rows = context.newResult(DSL.field("grade", Integer.class));
        rows.add(
            context.newRecord(DSL.field("grade", Integer.class)).value1(7)
        );
        try (
            MockConnection connection = new MockConnection(
                query -> new MockResult[] {new MockResult(1, rows)}
            )
        ) {
            MatcherAssert.assertThat(
                "The stored school grade was confused with the class identity",
                new ScPostgres(DSL.using(connection, SQLDialect.POSTGRES), 29L).grade(),
                Matchers.is(7)
            );
        }
    }

    @Test
    void reportsAnUnavailableClassWhenReadingItsGrade() throws Exception {
        try (
            MockConnection connection = new MockConnection(
                query -> new MockResult[] {
                    new MockResult(0, DSL.using(SQLDialect.POSTGRES).newResult()),
                }
            )
        ) {
            Assertions.assertThrows(
                SchoolClassNotFoundException.class,
                () -> new ScPostgres(DSL.using(connection, SQLDialect.POSTGRES), 31L).grade(),
                "Reading an unavailable class silently produced a grade"
            );
        }
    }
}
