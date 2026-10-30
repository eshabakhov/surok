/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum;

import com.eshabakhov.surok.media.MdXml;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.Test;

/**
 * Planning offers expose their requirements with nested federal subjects.
 * For example, each required subject prints its identity and name.
 *
 * @since 0.0.1
 */
final class CrOfferedTest {

    @Test
    void offersRequirementsWithoutMatchingSelections() throws Exception {
        final Result<Record> plans = DSL.using(SQLDialect.POSTGRES).fetchFromCSV(
            """
            id,title,education_level,study_week_type,version,academic_year,description
            61,План,BASIC,FIVE_DAYS,1,2026/2027,Описание
            """
        );
        final Result<Record> rows = DSL.using(SQLDialect.POSTGRES).fetchFromCSV(
            """
            id,grade,subject_id,hours,part_type
            71,5,29,2,MANDATORY
            73,6,31,3,MANDATORY
            """
        );
        final Result<Record> subjects = DSL.using(SQLDialect.POSTGRES).fetchFromCSV(
            """
            id,name,origin
            29,Музыка,FEDERAL
            31,МУЗЫКА,FEDERAL
            """
        );
        try (
            MockConnection connection = new MockConnection(
                query -> {
                    final Result<Record> result;
                    if (query.sql().contains("public.federal_curriculum_requirement")) {
                        result = rows.intoGroups(DSL.field("id", String.class))
                            .getOrDefault(query.bindings()[0].toString(), rows);
                    } else if (query.sql().contains("public.subject")) {
                        result = subjects.intoGroups(DSL.field("id", String.class))
                            .get(query.bindings()[0].toString());
                    } else {
                        result = plans;
                    }
                    return new MockResult[] {new MockResult(1, result)};
                }
            )
        ) {
            MatcherAssert.assertThat(
                "The offer duplicated canonical selections or omitted its original requirements",
                new CrOffered(new CrPostgres(DSL.using(connection, SQLDialect.POSTGRES), 61L))
                    .print(new MdXml("offer")).print(),
                Matchers.allOf(
                    Matchers.not(Matchers.containsString("<names>")),
                    Matchers.not(Matchers.containsString("<optional>")),
                    Matchers.containsString(
                        "<subject><id>29</id><name>Музыка</name><origin>FEDERAL</origin></subject>"
                    ),
                    Matchers.containsString(
                        "<subject><id>31</id><name>МУЗЫКА</name><origin>FEDERAL</origin></subject>"
                    )
                )
            );
        }
    }
}
