/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.autofill;

import com.eshabakhov.surok.federal.subject.SbFake;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.media.MdXml;
import com.eshabakhov.surok.school.schedule.load.hours.WhFixed;
import com.eshabakhov.surok.school.schoolclass.ScFake;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Immutable snapshots of prepared teaching loads and explicit decisions.
 * For example, iteration retains the original order and rejects duplicate pairs.
 *
 * @since 0.0.1
 */
final class PlsMemoryTest {

    @Test
    void rejectsDuplicatePreparedPairs() throws Exception {
        Assertions.assertThrows(
            LoadInputException.class,
            () -> new PlsMemory(
                List.of(
                    new PlInitial(
                        new ScFake(17L),
                        new SbFake(29L),
                        new WhFixed(5),
                        new WhFixed(3)
                    ),
                    new PlInitial(
                        new ScFake(17L),
                        new SbFake(29L),
                        new WhFixed(7),
                        new WhFixed(3)
                    )
                )
            ).iterate(),
            "Duplicate loads for the same class and subject were silently merged"
        );
    }

    @Test
    void copiesTheSuppliedCollection() throws Exception {
        final List<PlannedLoad> loads = new ArrayList<>(
            List.of(
                new PlInitial(
                    new ScFake(17L),
                    new SbFake(29L),
                    new WhFixed(5),
                    new WhFixed(3)
                )
            )
        );
        final PlannedLoads plan = new PlsMemory(loads);
        loads.clear();
        MatcherAssert.assertThat(
            "Changing the supplied collection changed the prepared plan",
            plan.iterate(),
            Matchers.contains(
                new PlInitial(
                    new ScFake(17L),
                    new SbFake(29L),
                    new WhFixed(5),
                    new WhFixed(3)
                )
            )
        );
    }

    @Test
    void preventsChangesThroughTheReturnedIterator() throws Exception {
        Assertions.assertThrows(
            UnsupportedOperationException.class,
            () -> {
                final Iterator<PlannedLoad> iterator = new PlsMemory(
                    List.of(
                        new PlInitial(
                            new ScFake(17L),
                            new SbFake(29L),
                            new WhFixed(5),
                            new WhFixed(3)
                        )
                    )
                ).iterate().iterator();
                iterator.next();
                iterator.remove();
            },
            "The returned iterator allowed removal of a prepared load"
        );
    }

    @Test
    void representsAnEmptyPlan() throws Exception {
        MatcherAssert.assertThat(
            "An empty preview introduced an unexpected load",
            new PlsMemory(List.of()).iterate(),
            Matchers.emptyIterable()
        );
    }

    @Test
    void printsPreparedLoadsAndTheirDecisions() throws Exception {
        final PlannedLoads plan = new PlsMemory(
            List.of(
                new PlInitial(
                    new ScFake(17L), new SbFake(29L), new WhFixed(5), new WhFixed(3)
                ).with(new WhFixed(3))
            )
        );
        MatcherAssert.assertThat(
            "The plan representation lost its current hours, proposal, or explicit decision",
            List.of(
                new PlsMemory(List.of()).print(new MdJson()).print(),
                plan.print(new MdJson()).print(),
                plan.print(new MdXml("plan")).print()
            ),
            Matchers.contains(
                JsonNodeFactory.instance.objectNode().set(
                    "items", JsonNodeFactory.instance.arrayNode()
                ),
                JsonNodeFactory.instance.objectNode().set(
                    "items", JsonNodeFactory.instance.arrayNode().add(
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
                                    .put("current", 5).put("proposed", 3).put("planned", 3)
                            )
                    )
                ),
                String.join(
                    "", "<plan><load><schoolClass><id>17</id></schoolClass>",
                    "<subject><id>29</id></subject>",
                    "<hours><current>5</current><proposed>3</proposed><planned>3</planned></hours>",
                    "<decision>manual</decision></load></plan>"
                )
            )
        );
    }
}
