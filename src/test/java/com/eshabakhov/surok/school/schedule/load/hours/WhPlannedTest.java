/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.hours;

import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.media.MdXml;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Selected hours preserve the original load and its optional curriculum proposal.
 * For example, removing a load changes only its planned hours to zero.
 *
 * @since 0.0.1
 */
final class WhPlannedTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 7})
    void selectsHoursWithoutChangingTheOriginal(final int selected) {
        final WeeklyHours origin = new WhProposed(new WhFixed(5), new WhFixed(3));
        MatcherAssert.assertThat(
            "Selecting hours changed the original value or lost the selected hours",
            List.of(new WhPlanned(origin, new WhFixed(selected)).value(), origin.value()),
            Matchers.contains(selected, 5)
        );
    }

    @ParameterizedTest
    @CsvSource({"3, 7", "3, 0", "0, 7", "0, 0"})
    void printsTheSelectedHoursWithTheOriginalContext(final int proposed, final int selected)
        throws Exception {
        final WeeklyHours hours = new WhPlanned(
            new WhProposed(new WhFixed(5), new WhFixed(proposed)), new WhFixed(selected)
        );
        final ObjectNode expected = JsonNodeFactory.instance.objectNode()
            .put("current", 5).put("planned", selected);
        String proposal = "";
        if (proposed > 0) {
            expected.put("proposed", proposed);
            proposal = String.format("<proposed>%d</proposed>", proposed);
        }
        MatcherAssert.assertThat(
            "The selected hours lost their context or duplicated the planned value",
            List.of(hours.print(new MdJson()).print(), hours.print(new MdXml("hours")).print()),
            Matchers.contains(
                expected, String.format(
                    "<hours><current>5</current>%s<planned>%d</planned></hours>", proposal, selected
                )
            )
        );
    }
}
