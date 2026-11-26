/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule;

import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.school.schedule.input.SdInput;
import com.eshabakhov.surok.school.schedule.input.SdInputName;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unsaved schedule fields used by creation forms.
 * For example, revised fields do not change the original draft.
 *
 * @since 0.0.1
 */
final class SdDraftTest {

    /**
     * Prints changed fields while keeping the original draft empty.
     *
     * @throws Exception If printing fails
     */
    @Test
    void printsRevisedFieldsWithoutChangingTheOriginal() throws Exception {
        final Schedule draft = new SdDraft(new SdInput());
        MatcherAssert.assertThat(
            "The draft lost its fields, changed its original state or acquired an identifier",
            List.of(
                draft.revise(new SdInputName(new SdInput(), "Осеннее расписание"))
                    .print(new MdJson()).print(),
                draft.print(new MdJson()).print()
            ),
            Matchers.contains(
                JsonNodeFactory.instance.objectNode().put("name", "Осеннее расписание"),
                JsonNodeFactory.instance.objectNode().put("name", "")
            )
        );
    }

    /** Rejects identity access before persistence. */
    @Test
    void refusesToInventAnIdentifier() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new SdDraft(new SdInput()).uid(),
            "The unsaved schedule exposed a database identifier"
        );
    }
}
