/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement.input;

import java.util.LinkedHashMap;
import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A single form selector distinguishes federal subjects from an additional hours limit.
 * For example, OPTIONAL becomes an optional requirement without a subject identifier.
 *
 * @since 0.0.1
 */
final class RqInputFormTest {

    @ParameterizedTest
    @CsvSource({"OPTIONAL, 0, OPTIONAL", "OPTIONAL, 3, OPTIONAL", "517, 7, MANDATORY"})
    void printsTheSelectedRequirement(final String selection, final String hours, final String part)
        throws Exception {
        final Map<String, String> expected = new LinkedHashMap<>(
            Map.of("grade", "5", "hours", hours, "partType", part)
        );
        if ("MANDATORY".equals(part)) {
            expected.put("subjectId", selection);
        }
        MatcherAssert.assertThat(
            "The form selection changed the requirement kind, subject or hours",
            new RqInputForm(
                Map.of("grade", "5", "selection", selection, "hours", hours)
            ).print(new MdRqInput()).print(),
            Matchers.equalTo(expected)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "Неизвестный предмет", "0", "-1"})
    void rejectsInvalidSelections(final String selection) {
        Assertions.assertThrows(
            RequirementInputException.class,
            () -> new RqInputForm(
                Map.of("grade", "5", "selection", selection, "hours", "3")
            ).print(new MdRqInput()),
            "The form accepted an empty or invalid subject selection"
        );
    }
}
