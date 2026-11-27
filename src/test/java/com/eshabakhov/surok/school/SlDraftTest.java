/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.school.input.SlInput;
import com.eshabakhov.surok.school.input.SlInputName;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unsaved school fields used by creation forms.
 * For example, revised fields do not change the original draft.
 *
 * @since 0.0.1
 */
final class SlDraftTest {

    /**
     * Prints changed fields while keeping the original draft empty.
     *
     * @throws Exception If printing fails
     */
    @Test
    void printsRevisedFieldsWithoutChangingTheOriginal() throws Exception {
        final School draft = new SlDraft(new SlInput());
        MatcherAssert.assertThat(
            "The draft lost its fields, changed its original state or acquired an identifier",
            List.of(
                draft.revise(new SlInputName(new SlInput(), "Лицей «Восток»"))
                    .print(new MdJson()).print(),
                draft.print(new MdJson()).print()
            ),
            Matchers.contains(
                JsonNodeFactory.instance.objectNode().put("name", "Лицей «Восток»"),
                JsonNodeFactory.instance.objectNode().put("name", "")
            )
        );
    }

    /** Rejects identity access before persistence. */
    @Test
    void refusesToInventAnIdentifier() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new SlDraft(new SlInput()).uid(),
            "The unsaved school exposed a database identifier"
        );
    }
}
