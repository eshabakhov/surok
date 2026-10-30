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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Planned teaching loads and successive user decisions.
 *
 * @since 0.0.1
 */
final class PlInitialTest {

    @ParameterizedTest
    @CsvSource({"0, 3, 3", "5, 3, 5", "5, 0, 5", "0, 0, 0"})
    void printsCurrentAndProposedHours(
        final int current, final int proposed, final int expected
    ) throws Exception {
        final ObjectNode hours = JsonNodeFactory.instance.objectNode()
            .put("current", current).put("planned", expected);
        if (proposed > 0) {
            hours.put("proposed", proposed);
        }
        MatcherAssert.assertThat(
            "The initial representation lost the current hours or curriculum proposal",
            new PlInitial(
                new ScFake(11L),
                new SbFake(22L),
                new WhFixed(current),
                new WhFixed(proposed)
            ).print(new MdJson())
                .print(),
            Matchers.equalTo(
                JsonNodeFactory.instance.objectNode()
                    .<ObjectNode>set(
                        "schoolClass", JsonNodeFactory.instance.objectNode().put("id", 11L)
                    )
                    .<ObjectNode>set(
                        "subject", JsonNodeFactory.instance.objectNode().put("id", 22L)
                    )
                    .put("decision", "none")
                    .<ObjectNode>set("hours", hours)
            )
        );
    }

    @Test
    void acceptsTheProposalWithoutChangingTheOriginal() throws Exception {
        final PlannedLoad origin = new PlInitial(
            new ScFake(11L),
            new SbFake(22L),
            new WhFixed(5),
            new WhFixed(3)
        );
        MatcherAssert.assertThat(
            "Accepting the proposal altered the original load",
            List.of(
                origin.accept(),
                origin
            ),
            Matchers.contains(
                new PlDecided(
                    new PlInitial(
                        new ScFake(11L),
                        new SbFake(22L),
                        new WhFixed(5),
                        new WhFixed(3)
                    ),
                    new WhFixed(3),
                    "plan"
                ),
                new PlInitial(
                    new ScFake(11L),
                    new SbFake(22L),
                    new WhFixed(5),
                    new WhFixed(3)
                )
            )
        );
    }

    @Test
    void replacesAnEarlierDecisionWithManualHours() throws Exception {
        MatcherAssert.assertThat(
            "Manual editing failed to replace the previous curriculum decision",
            new PlInitial(
                new ScFake(11L),
                new SbFake(22L),
                new WhFixed(5),
                new WhFixed(3)
            ).accept().with(new WhFixed(7)),
            Matchers.is(
                new PlDecided(
                    new PlInitial(
                        new ScFake(11L),
                        new SbFake(22L),
                        new WhFixed(5),
                        new WhFixed(3)
                    ),
                    new WhFixed(7),
                    "manual"
                )
            )
        );
    }

    @Test
    void removesTheLoadWithoutLosingCurrentOrProposedHours() throws Exception {
        MatcherAssert.assertThat(
            "Removal lost the original hours or curriculum proposal",
            new PlInitial(
                new ScFake(11L),
                new SbFake(22L),
                new WhFixed(5),
                new WhFixed(3)
            ).accept().with(new WhFixed(0)),
            Matchers.is(
                new PlDecided(
                    new PlInitial(
                        new ScFake(11L),
                        new SbFake(22L),
                        new WhFixed(5),
                        new WhFixed(3)
                    ),
                    new WhFixed(0),
                    "removed"
                )
            )
        );
    }

    @Test
    void treatsZeroHoursAsRemoval() throws Exception {
        MatcherAssert.assertThat(
            "Zero manual hours did not mark the load for removal",
            new PlInitial(
                new ScFake(11L),
                new SbFake(22L),
                new WhFixed(5),
                new WhFixed(3)
            ).with(new WhFixed(0)),
            Matchers.is(
                new PlDecided(
                    new PlInitial(
                        new ScFake(11L),
                        new SbFake(22L),
                        new WhFixed(5),
                        new WhFixed(3)
                    ),
                    new WhFixed(0),
                    "removed"
                )
            )
        );
    }

    @Test
    void acceptsTheProposalAfterAnEarlierRemoval() throws Exception {
        MatcherAssert.assertThat(
            "The curriculum decision failed to replace the previous removal",
            new PlInitial(
                new ScFake(11L),
                new SbFake(22L),
                new WhFixed(5),
                new WhFixed(3)
            ).with(new WhFixed(0)).accept(),
            Matchers.is(
                new PlDecided(
                    new PlInitial(
                        new ScFake(11L),
                        new SbFake(22L),
                        new WhFixed(5),
                        new WhFixed(3)
                    ),
                    new WhFixed(3),
                    "plan"
                )
            )
        );
    }

    @Test
    void allowsManualHoursWithoutACurriculumProposal() throws Exception {
        MatcherAssert.assertThat(
            "Manual editing failed without a curriculum proposal",
            new PlInitial(
                new ScFake(11L),
                new SbFake(22L),
                new WhFixed(5),
                new WhFixed(0)
            ).with(new WhFixed(7)),
            Matchers.is(
                new PlDecided(
                    new PlInitial(
                        new ScFake(11L),
                        new SbFake(22L),
                        new WhFixed(5),
                        new WhFixed(0)
                    ),
                    new WhFixed(7),
                    "manual"
                )
            )
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 5})
    void rejectsAcceptingAnAbsentProposal(final int current) {
        Assertions.assertThrows(
            LoadInputException.class,
            () -> new PlInitial(
                new ScFake(11L),
                new SbFake(22L),
                new WhFixed(current),
                new WhFixed(0)
            ).accept(),
            "A curriculum decision was accepted without a proposal"
        );
    }

    @Test
    void rejectsNegativeManualHours() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new PlInitial(
                new ScFake(11L),
                new SbFake(22L),
                new WhFixed(5),
                new WhFixed(3)
            ).with(new WhFixed(-1)),
            "Negative manual hours were accepted"
        );
    }

    @Test
    void writesAnAbsentLoadWithProposedHours() throws Exception {
        final List<List<Object>> written = new ArrayList<>(0);
        new PlInitial(
            new ScFake(17L), new SbFake(29L), new WhFixed(0), new WhFixed(3)
        ).apply(
            (clazz, subject, hours) -> written.add(List.of(clazz, subject, hours))
        );
        MatcherAssert.assertThat(
            "An absent load was not written with the proposed hours and original identities",
            written,
            Matchers.contains(List.of(new ScFake(17L), new SbFake(29L), new WhFixed(3)))
        );
    }

    @ParameterizedTest
    @CsvSource({"5, 3", "5, 0", "0, 0"})
    void leavesExistingAndUnproposedLoadsUnwritten(final int current, final int proposed)
        throws Exception {
        final List<List<Object>> written = new ArrayList<>(0);
        new PlInitial(
            new ScFake(17L), new SbFake(29L), new WhFixed(current), new WhFixed(proposed)
        ).apply(
            (clazz, subject, hours) -> written.add(List.of(clazz, subject, hours))
        );
        MatcherAssert.assertThat(
            "An undecided load overwrote existing hours or created an empty proposal",
            written,
            Matchers.empty()
        );
    }
}
