/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schoolclass;

import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.school.schoolclass.input.ScInput;
import com.eshabakhov.surok.school.schoolclass.input.ScInputGrade;
import com.eshabakhov.surok.school.schoolclass.input.ScInputLitera;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unsaved class fields, including blank grades before entry.
 * For example, revising a draft gives it a grade without a database identity.
 *
 * @since 0.0.1
 */
final class ScDraftTest {

    /**
     * Prints the same fields as the class form without changing its original draft.
     *
     * @throws Exception If printing fails
     */
    @Test
    void printsEmptyAndRevisedClassFields() throws Exception {
        final SchoolClass draft = new ScDraft(new ScInput());
        MatcherAssert.assertThat(
            "The class draft lost its fields or changed its original state",
            List.of(
                draft.revise(new ScInputLitera(new ScInputGrade(new ScInput(), "7"), "Б"))
                    .print(new MdJson()).print(),
                draft.print(new MdJson()).print()
            ),
            Matchers.contains(
                JsonNodeFactory.instance.objectNode().put("name", "7Б")
                    .put("grade", "7").put("litera", "Б"),
                JsonNodeFactory.instance.objectNode().put("name", "")
                    .put("grade", "").put("litera", "")
            )
        );
    }

    /**
     * Reads a grade entered before persistence.
     *
     * @throws Exception If reading fails
     */
    @Test
    void usesTheEnteredGrade() throws Exception {
        MatcherAssert.assertThat(
            "The class draft lost the entered grade",
            new ScDraft(new ScInputGrade(new ScInput(), "7")).grade(), Matchers.is(7)
        );
    }

    /** Rejects a grade lookup before any grade was entered. */
    @Test
    void refusesAnUnspecifiedGrade() {
        Assertions.assertThrows(
            IllegalStateException.class, () -> new ScDraft(new ScInput()).grade(),
            "The empty class draft invented a grade"
        );
    }
}
