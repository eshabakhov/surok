/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Subject names remain available through the versioned representation.
 * For example, a versioned subject can participate in automatic load matching.
 *
 * @since 0.0.1
 */
final class SbV1Test {

    @Test
    void preservesTheSubjectNameForMatching() throws Exception {
        MatcherAssert.assertThat(
            "The versioned subject lost its original name",
            new SbV1(
                new SbFake(29L, " МАТЕМАТИКА ")
            ).name(),
            Matchers.is(" МАТЕМАТИКА ")
        );
    }
}
