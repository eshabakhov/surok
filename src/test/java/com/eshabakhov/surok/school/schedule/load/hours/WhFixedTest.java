/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.hours;

import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.media.MdXml;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Nonnegative weekly teaching hours.
 *
 * @since 0.0.1
 */
final class WhFixedTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 7})
    void printsHoursWithoutAssigningAPlanningState(final int hours) {
        MatcherAssert.assertThat(
            "Fixed hours introduced a planning state or changed their numeric representation",
            List.of(
                new WhFixed(hours).print(new MdJson()).print(),
                new WhFixed(hours).print(new MdXml("load")).print()
            ),
            Matchers.contains(
                JsonNodeFactory.instance.objectNode().put("hours", hours),
                String.format("<load><hours>%d</hours></load>", hours)
            )
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 5, Integer.MAX_VALUE})
    void evaluatesNonnegativeHours(final int hours) {
        MatcherAssert.assertThat(
            "Evaluating weekly hours changed the supplied number",
            new WhFixed(hours).value(),
            Matchers.is(hours)
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, -1})
    void rejectsNegativeHoursWhenEvaluated(final int hours) {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new WhFixed(hours).value(),
            "Negative weekly hours were accepted"
        );
    }
}
