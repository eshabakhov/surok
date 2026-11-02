/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schoolclass;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * School grades remain available through the versioned representation.
 * For example, a versioned class can participate in curriculum-based load planning.
 *
 * @since 0.0.1
 */
final class ScV1Test {

    @Test
    void preservesTheSchoolGradeForPlanning() throws Exception {
        MatcherAssert.assertThat(
            "The versioned class lost its school grade",
            new ScV1(
                new ScFake(29L, 7)
            ).grade(),
            Matchers.is(7)
        );
    }
}
