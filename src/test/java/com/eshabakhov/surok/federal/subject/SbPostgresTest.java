/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

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
 * Typed subject names read directly from database records.
 * For example, reading a name preserves the display text stored in the catalogue.
 *
 * @since 0.0.1
 */
final class SbPostgresTest {

    @Test
    void preservesTheNameReadFromTheDatabase() throws Exception {
        final DSLContext context = DSL.using(SQLDialect.POSTGRES);
        final Result<Record1<String>> rows = context.newResult(DSL.field("name", String.class));
        rows.add(
            context.newRecord(DSL.field("name", String.class)).value1(" ИНОСТРАННЫЙ  ЯЗЫК ")
        );
        try (
            MockConnection connection = new MockConnection(
                query -> new MockResult[] {new MockResult(1, rows)}
            )
        ) {
            MatcherAssert.assertThat(
                "Reading the subject changed its original display name",
                new SbPostgres(DSL.using(connection, SQLDialect.POSTGRES), 29L).name(),
                Matchers.is(" ИНОСТРАННЫЙ  ЯЗЫК ")
            );
        }
    }

    @Test
    void reportsAnUnavailableSubjectWhenReadingItsName() throws Exception {
        try (
            MockConnection connection = new MockConnection(
                query -> new MockResult[] {
                    new MockResult(0, DSL.using(SQLDialect.POSTGRES).newResult()),
                }
            )
        ) {
            Assertions.assertThrows(
                SubjectNotFoundException.class,
                () -> new SbPostgres(DSL.using(connection, SQLDialect.POSTGRES), 31L).name(),
                "Reading an unavailable subject silently produced a name"
            );
        }
    }
}
