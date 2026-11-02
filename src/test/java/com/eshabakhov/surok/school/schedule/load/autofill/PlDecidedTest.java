/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.autofill;

import com.eshabakhov.surok.federal.subject.SbFake;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.school.schedule.load.hours.WhFixed;
import com.eshabakhov.surok.school.schoolclass.ScFake;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Planned load decisions retaining their original facts and user intent.
 * For example, equal resulting hours do not make manual and curriculum choices equal.
 *
 * @since 0.0.1
 */
final class PlDecidedTest {

    @Test
    void distinguishesManualAndCurriculumDecisionsWithTheSameHours() throws Exception {
        MatcherAssert.assertThat(
            "Manual and curriculum decisions with equal hours were treated as the same choice",
            new PlInitial(
                new ScFake(17L),
                new SbFake(29L),
                new WhFixed(3),
                new WhFixed(3)
            ).with(new WhFixed(3)),
            Matchers.not(
                Matchers.is(
                    new PlInitial(
                        new ScFake(17L),
                        new SbFake(29L),
                        new WhFixed(3),
                        new WhFixed(3)
                    ).accept()
                )
            )
        );
    }

    @Test
    void retainsTheOriginalHoursWhenComparingDecisions() throws Exception {
        MatcherAssert.assertThat(
            "Comparing decisions lost the original teaching load",
            new PlInitial(
                new ScFake(17L),
                new SbFake(29L),
                new WhFixed(5),
                new WhFixed(3)
            ).accept(),
            Matchers.not(
                Matchers.is(
                    new PlInitial(
                        new ScFake(17L),
                        new SbFake(29L),
                        new WhFixed(7),
                        new WhFixed(3)
                    ).accept()
                )
            )
        );
    }

    @Test
    void printsTheDecisionAlongsideTheOriginalHoursAndProposal() throws Exception {
        MatcherAssert.assertThat(
            "The printed decision lost its original facts or selected hours",
            new PlDecided(
                new PlInitial(
                    new ScFake(17L),
                    new SbFake(29L),
                    new WhFixed(5),
                    new WhFixed(3)
                ),
                new WhFixed(7),
                "manual"
            ).print(new MdJson())
                .print(),
            Matchers.equalTo(
                JsonNodeFactory.instance.objectNode()
                    .<ObjectNode>set(
                        "schoolClass", JsonNodeFactory.instance.objectNode().put("id", 17L)
                    )
                    .<ObjectNode>set(
                        "subject", JsonNodeFactory.instance.objectNode().put("id", 29L)
                    )
                    .put("decision", "manual")
                    .<ObjectNode>set(
                        "hours", JsonNodeFactory.instance.objectNode()
                            .put("current", 5).put("proposed", 3).put("planned", 7)
                    )
            )
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 7})
    void writesTheExplicitHoursIncludingRemoval(final int value) throws Exception {
        final List<List<Object>> written = new ArrayList<>(0);
        new PlInitial(
            new ScFake(17L), new SbFake(29L), new WhFixed(5), new WhFixed(3)
        ).with(new WhFixed(value)).apply(
            (clazz, subject, hours) -> written.add(List.of(clazz, subject, hours))
        );
        MatcherAssert.assertThat(
            "An explicit decision wrote the wrong pair or hour value",
            written,
            Matchers.contains(List.of(new ScFake(17L), new SbFake(29L), new WhFixed(value)))
        );
    }
}
