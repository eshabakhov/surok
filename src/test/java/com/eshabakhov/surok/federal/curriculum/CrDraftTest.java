/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum;

import com.eshabakhov.surok.federal.curriculum.input.CrInput;
import com.eshabakhov.surok.federal.curriculum.input.CrInputLevel;
import com.eshabakhov.surok.federal.curriculum.input.CrInputTitle;
import com.eshabakhov.surok.media.MdJson;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.List;
import java.util.stream.IntStream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Curriculum form fields before the plan is saved.
 * For example, changing the level changes the available grades in the draft.
 *
 * @since 0.0.1
 */
final class CrDraftTest {

    /**
     * Prints form defaults and entered values without creating an identifier.
     *
     * @throws Exception If printing fails
     */
    @Test
    void printsDefaultsAndRevisedFields() throws Exception {
        final Curriculum draft = new CrDraft(new CrInput());
        MatcherAssert.assertThat(
            "The curriculum draft lost its defaults or changed its original fields",
            List.of(
                draft.revise(new CrInputTitle(new CrInput(), "Учебный план"))
                    .print(new MdJson()).print(),
                draft.print(new MdJson()).print()
            ),
            Matchers.contains(
                JsonNodeFactory.instance.objectNode().put("title", "Учебный план")
                    .put("level", "PRIMARY").put("week", "FIVE_DAYS").put("version", "")
                    .put("year", "").put("description", ""),
                JsonNodeFactory.instance.objectNode().put("title", "")
                    .put("level", "PRIMARY").put("week", "FIVE_DAYS").put("version", "")
                    .put("year", "").put("description", "")
            )
        );
    }

    /**
     * Selects grades from the entered level.
     *
     * @param level Curriculum level
     * @param first First grade
     * @param last Last grade
     * @throws Exception If reading fails
     */
    @ParameterizedTest
    @CsvSource({"PRIMARY, 1, 4", "BASIC, 5, 9", "SECONDARY, 10, 11"})
    void selectsGradesForTheEnteredLevel(final String level, final int first, final int last)
        throws Exception {
        MatcherAssert.assertThat(
            "The curriculum draft offered grades outside its level",
            new CrDraft(new CrInputLevel(new CrInput(), level)).grades(),
            Matchers.is(IntStream.rangeClosed(first, last).boxed().toList())
        );
    }
}
