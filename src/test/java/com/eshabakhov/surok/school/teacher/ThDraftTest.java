/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.teacher;

import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.school.teacher.input.ThInput;
import com.eshabakhov.surok.school.teacher.input.ThInputName;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unsaved teacher fields used by creation forms.
 * For example, revised fields do not change the original draft.
 *
 * @since 0.0.1
 */
final class ThDraftTest {

    /**
     * Prints changed fields while keeping the original draft empty.
     *
     * @throws Exception If printing fails
     */
    @Test
    void printsRevisedFieldsWithoutChangingTheOriginal() throws Exception {
        final Teacher draft = new ThDraft(new ThInput());
        MatcherAssert.assertThat(
            "The draft lost its fields, changed its original state or acquired an identifier",
            List.of(
                draft.revise(new ThInputName(new ThInput(), "Мария Ли"))
                    .print(new MdJson()).print(),
                draft.print(new MdJson()).print()
            ),
            Matchers.contains(
                JsonNodeFactory.instance.objectNode().put("name", "Мария Ли"),
                JsonNodeFactory.instance.objectNode().put("name", "")
            )
        );
    }

    /** Rejects identity access before persistence. */
    @Test
    void refusesToInventAnIdentifier() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new ThDraft(new ThInput()).uid(),
            "The unsaved teacher exposed a database identifier"
        );
    }
}
