/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.hours;

import com.eshabakhov.surok.media.MdJson;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Current hours and optional proposals produce an initial weekly load.
 * For example, an absent load takes the proposal while existing hours are preserved.
 *
 * @since 0.0.1
 */
final class WhProposedTest {

    @ParameterizedTest
    @CsvSource({"0, 3, 3", "5, 3, 5", "5, 0, 5", "0, 0, 0"})
    void preservesExistingHoursOrUsesTheProposal(
        final int current, final int proposed, final int expected
    ) {
        MatcherAssert.assertThat(
            "The initial hours replaced an existing load or lost the proposal",
            new WhProposed(new WhFixed(current), new WhFixed(proposed)).value(),
            Matchers.is(expected)
        );
    }

    @ParameterizedTest
    @CsvSource({"0, 3, 3", "5, 3, 5", "5, 0, 5", "0, 0, 0"})
    void printsCurrentHoursAndOnlyAnExistingProposal(
        final int current, final int proposed, final int planned
    ) {
        final ObjectNode expected = JsonNodeFactory.instance.objectNode()
            .put("current", current).put("planned", planned);
        if (proposed > 0) {
            expected.put("proposed", proposed);
        }
        MatcherAssert.assertThat(
            "The initial hours lost their context or printed a nonexistent proposal",
            new WhProposed(new WhFixed(current), new WhFixed(proposed)).print(new MdJson()).print(),
            Matchers.equalTo(expected)
        );
    }
}
