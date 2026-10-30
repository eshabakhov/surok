/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement.input;

import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link RqInputHours}.
 *
 * @since 0.0.1
 */
final class RqInputHoursTest {

    @ParameterizedTest
    @ValueSource(strings = {" 7 ", "0", " 2147483647 "})
    void printsIntegerHours(final String hours) throws Exception {
        MatcherAssert.assertThat(
            "Hours must be parsed and printed without surrounding spaces",
            new RqInputHours(new RqInput(), hours)
                .print(new MdRqInput()).print(),
            Matchers.is(Map.of("hours", hours.trim()))
        );
    }

    @Test
    void rejectsNullHours() {
        Assertions.assertThrows(
            RequirementInputException.class,
            () -> new RqInputHours(new RqInput(), null)
                .print(new MdRqInput())
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void rejectsBlankHours(final String hours) {
        Assertions.assertThrows(
            RequirementInputException.class,
            () -> new RqInputHours(new RqInput(), hours)
                .print(new MdRqInput())
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"four", "2147483648", "-2147483649", "-1", " -2147483648 "})
    void rejectsInvalidHours(final String hours) {
        Assertions.assertThrows(
            RequirementInputException.class,
            () -> new RqInputHours(new RqInput(), hours)
                .print(new MdRqInput())
        );
    }
}
