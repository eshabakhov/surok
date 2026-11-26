/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.building.cabinet;

import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.school.building.cabinet.input.CbInput;
import com.eshabakhov.surok.school.building.cabinet.input.CbInputName;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unsaved cabinet fields used by creation forms.
 * For example, revised fields do not change the original draft.
 *
 * @since 0.0.1
 */
final class CbDraftTest {

    /**
     * Prints changed fields while keeping the original draft empty.
     *
     * @throws Exception If printing fails
     */
    @Test
    void printsRevisedFieldsWithoutChangingTheOriginal() throws Exception {
        final Cabinet draft = new CbDraft(new CbInput());
        MatcherAssert.assertThat(
            "The draft lost its fields, changed its original state or acquired an identifier",
            List.of(
                draft.revise(new CbInputName(new CbInput(), "Кабинет «А»"))
                    .print(new MdJson()).print(),
                draft.print(new MdJson()).print()
            ),
            Matchers.contains(
                JsonNodeFactory.instance.objectNode().put("name", "Кабинет «А»"),
                JsonNodeFactory.instance.objectNode().put("name", "")
            )
        );
    }

    /** Rejects identity access before persistence. */
    @Test
    void refusesToInventAnIdentifier() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new CbDraft(new CbInput()).uid(),
            "The unsaved cabinet exposed a database identifier"
        );
    }
}
