/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load;

import com.eshabakhov.surok.media.MdJson;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Stored teaching loads printed with their class and subject names.
 * For example, historical assignments retain the names needed for display.
 *
 * @since 0.0.1
 */
final class ClassLoadPostgresTest {

    @Test
    void printsThePersistedLoad() throws Exception {
        try (
            MockConnection connection = new MockConnection(
                query -> new MockResult[] {
                    new MockResult(
                        1, DSL.using(SQLDialect.POSTGRES).fetchFromCSV(
                            Map.of(
                                41L, """
                                id,school_class_id,subject_id,hours
                                41,17,29,7
                                """,
                                17L, """
                                id,grade,litera
                                17,5,Б
                                """,
                                29L, """
                                id,name,origin
                                29,Математика,FEDERAL
                                """
                            ).get(query.bindings()[0])
                        )
                    ),
                }
            )
        ) {
            MatcherAssert.assertThat(
                "Reading a stored load confused its identity with its class, subject, or hours",
                new ClassLoadPostgres(
                    DSL.using(connection, SQLDialect.POSTGRES), 41L
                ).print(new MdJson()).print(),
                Matchers.equalTo(
                    JsonNodeFactory.instance.objectNode()
                        .put("id", 41L).put("hours", 7).setAll(
                            Map.of(
                                "schoolClass", JsonNodeFactory.instance.objectNode().put("id", 17L)
                                    .put("name", "5Б").put("grade", 5).put("litera", "Б"),
                                "subject", JsonNodeFactory.instance.objectNode().put("id", 29L)
                                    .put("name", "Математика").put("origin", "FEDERAL")
                            )
                        )
                )
            );
        }
    }

    @Test
    void rejectsPrintingAMissingLoad() throws Exception {
        try (
            MockConnection connection = new MockConnection(
                query -> new MockResult[] {
                    new MockResult(0, DSL.using(SQLDialect.POSTGRES).newResult()),
                }
            )
        ) {
            Assertions.assertThrows(
                LoadNotFoundException.class,
                () -> new ClassLoadPostgres(
                    DSL.using(connection, SQLDialect.POSTGRES), 43L
                ).print(new MdJson()),
                "A missing stored load silently produced a representation"
            );
        }
    }
}
