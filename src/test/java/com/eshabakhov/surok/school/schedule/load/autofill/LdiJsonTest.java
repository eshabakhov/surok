/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.autofill;

import com.eshabakhov.surok.school.schedule.load.hours.WhFixed;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Typed planning input without class selections or matching decisions.
 * For example, duplicate grade or subject entries are rejected before saving.
 *
 * @since 0.0.1
 */
final class LdiJsonTest {

    @ParameterizedTest
    @ValueSource(
        strings = {
            "{}", "{\"curriculumId\":61}", "{\"curriculum\":61}",
            "{\"curriculum\":null}", "{\"curriculum\":{}}", "{\"curriculum\":[]}",
            "{\"curriculum\":{\"id\":0}}", "{\"curriculum\":{\"id\":-61}}",
            "{\"curriculum\":{\"id\":1.5}}", "{\"curriculum\":{\"id\":\"61\"}}",
            "{\"curriculum\":{\"id\":9223372036854775808}}"
        }
    )
    void rejectsInvalidCurricula(final String document) {
        Assertions.assertThrows(
            LoadInputException.class,
            () -> new LdiJson(DSL.using(SQLDialect.POSTGRES), new ObjectMapper().readTree(document))
                .curriculum(),
            "Malformed curriculum identifiers were accepted"
        );
    }

    @Test
    void decodesTheNestedCurriculumReference() throws Exception {
        MatcherAssert.assertThat(
            "The nested curriculum reference lost its identity",
            new LdiJson(
                DSL.using(SQLDialect.POSTGRES), new ObjectMapper().readTree(
                    "{\"curriculum\":{\"id\":61}}"
                )
            ).curriculum().uid(),
            Matchers.is(61L)
        );
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "{\"hours\":2}", "{\"subjectId\":29,\"hours\":2}",
            "{\"subject\":29,\"hours\":2}",
            "{\"subject\":null,\"hours\":2}",
            "{\"subject\":{},\"hours\":2}",
            "{\"subject\":[],\"hours\":2}",
            "{\"subject\":{\"id\":0},\"hours\":2}",
            "{\"subject\":{\"id\":-29},\"hours\":2}",
            "{\"subject\":{\"id\":1.5},\"hours\":2}",
            "{\"subject\":{\"id\":\"29\"},\"hours\":2}",
            "{\"subject\":{\"id\":9223372036854775808},\"hours\":2}"
        }
    )
    void rejectsInvalidSubjectReferences(final String reference) {
        Assertions.assertThrows(
            LoadInputException.class,
            () -> new LdiJson(
                DSL.using(SQLDialect.POSTGRES), new ObjectMapper().readTree(
                    String.format(
                        "{\"additions\":[{\"grade\":5,\"subjects\":[%s]}]}", reference
                    )
                )
            ).additions(),
            "Malformed subject references were accepted"
        );
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "{}", "{\"additions\":{}}", "{\"additions\":[{\"grade\":5}]}",
            "{\"additions\":[{\"grade\":-1,\"subjects\":[]}]}",
            "{\"additions\":[{\"grade\":5,\"subjects\":[]},{\"grade\":5,\"subjects\":[]}]}",
            "{\"additions\":[],\"classIds\":[]}", "{\"additions\":[],\"mappings\":[]}",
            "{\"additions\":[],\"changes\":[]}", "{\"additions\":[],\"optionalSubjects\":[]}",
            """
            {"additions":[{"grade":5,"subjects":[{"subject":{"id":29},"hours":1},
                {"subject":{"id":29},"hours":2}]}]}
            """,
            """
            {"additions":[{"grade":5,"subjects":[{"subject":{"id":29},"hours":-1}]}]}
            """,
            """
            {"additions":[{"grade":5,"subjects":[{"subject":{"id":29},"hours":1.5}]}]}
            """
        }
    )
    void rejectsMalformedOrObsoleteSelections(final String document) {
        Assertions.assertThrows(
            LoadInputException.class,
            () -> new LdiJson(DSL.using(SQLDialect.POSTGRES), new ObjectMapper().readTree(document))
                .additions(),
            "Malformed or obsolete draft selections were accepted"
        );
    }

    @Test
    void decodesManualHoursForAGrade() throws Exception {
        MatcherAssert.assertThat(
            "The typed grade selection lost its weekly hours",
            new LdiJson(
                DSL.using(SQLDialect.POSTGRES), new ObjectMapper().readTree(
                    """
                    {"curriculum":{"id":61},"additions":[{"grade":5,
                        "subjects":[{"subject":{"id":29},"hours":2}]}]}
                    """
                )
            ).additions().get(5).values(),
            Matchers.contains(new WhFixed(2))
        );
    }
}
