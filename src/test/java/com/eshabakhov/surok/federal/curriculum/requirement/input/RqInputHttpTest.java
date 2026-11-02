/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement.input;

import java.util.LinkedHashMap;
import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Test for {@link RqInputHttp}.
 *
 * @since 0.0.1
 */
final class RqInputHttpTest {

    @ParameterizedTest
    @CsvSource({"MANDATORY, 0", "MANDATORY, -1", "OPTIONAL, -1", "OPTIONAL, 1.5"})
    void rejectsHoursOutsideTheRequirementRules(final String part, final String hours) {
        final Map<String, String> fields = new LinkedHashMap<>(
            Map.of("grade", "5", "partType", part, "hours", hours)
        );
        if ("MANDATORY".equals(part)) {
            fields.put("subjectId", "517");
        }
        Assertions.assertThrows(
            RequirementInputException.class,
            () -> new RqInputHttp(fields).print(new MdRqInput()),
            "The requirement accepted hours outside its allowed range"
        );
    }

    @Test
    void printsFirstHttpValues() throws Exception {
        final MultiValueMap<String, String> parameters =
            new LinkedMultiValueMap<>();
        parameters.add("grade", " 5 ");
        parameters.add("grade", "6");
        parameters.add("subjectId", "504");
        parameters.add("hours", " 4 ");
        parameters.add("partType", " MANDATORY ");
        MatcherAssert.assertThat(
            "HTTP input must compose all requirement fields",
            new RqInputHttp(parameters).print(new MdRqInput()).print(),
            Matchers.is(
                Map.of(
                    "grade", "5",
                    "subjectId", "504",
                    "hours", "4",
                    "partType", "MANDATORY"
                )
            )
        );
    }

    @Test
    void rejectsMissingHttpValues() {
        Assertions.assertThrows(
            RequirementInputException.class,
            () -> new RqInputHttp(new LinkedMultiValueMap<>(0))
                .print(new MdRqInput())
        );
    }

    @Test
    void rejectsNullParameters() {
        final Map<String, String> parameters = null;
        Assertions.assertThrows(
            NullPointerException.class,
            () -> new RqInputHttp(parameters).print(new MdRqInput())
        );
    }
}
