/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

import com.eshabakhov.surok.federal.subject.input.SbInput;
import com.eshabakhov.surok.federal.subject.input.SbInputName;
import com.eshabakhov.surok.media.MdJson;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unsaved subject fields used by creation forms.
 * For example, revised fields do not change the original draft.
 *
 * @since 0.0.1
 */
final class SbDraftTest {

    /**
     * Prints changed fields while keeping the original draft empty.
     *
     * @throws Exception If printing fails
     */
    @Test
    void printsRevisedFieldsWithoutChangingTheOriginal() throws Exception {
        final Subject draft = new SbDraft(new SbInput(), Subject.Origin.SCHOOL);
        MatcherAssert.assertThat(
            "The draft lost its fields, changed its original state or acquired an identifier",
            List.of(
                draft.revise(new SbInputName(new SbInput(), "Робототехника"))
                    .print(new MdJson()).print(),
                draft.print(new MdJson()).print()
            ),
            Matchers.contains(
                JsonNodeFactory.instance.objectNode().put("name", "Робототехника")
                    .put("origin", "SCHOOL"),
                JsonNodeFactory.instance.objectNode().put("name", "").put("origin", "SCHOOL")
            )
        );
    }

    /** Rejects identity access before persistence. */
    @Test
    void refusesToInventAnIdentifier() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new SbDraft(new SbInput(), Subject.Origin.SCHOOL).uid(),
            "The unsaved subject exposed a database identifier"
        );
    }
}
