/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.building;

import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.school.building.input.BdInput;
import com.eshabakhov.surok.school.building.input.BdInputName;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unsaved building fields without a database identity.
 * For example, revising a draft preserves the original empty fields.
 *
 * @since 0.0.1
 */
final class BdDraftTest {

    /**
     * Prints revised fields without changing the original draft or inventing an identifier.
     *
     * @param name Building name
     * @throws Exception If printing fails
     */
    @ParameterizedTest
    @ValueSource(strings = {"Северный корпус", "Флигель <А> & Б"})
    void printsRevisedFieldsWithoutChangingTheOriginal(final String name) throws Exception {
        final Building draft = new BdDraft(new BdInput());
        MatcherAssert.assertThat(
            "The draft lost its fields, changed its original state or acquired an identifier",
            List.of(
                draft.revise(new BdInputName(new BdInput(), name)).print(new MdJson()).print(),
                draft.print(new MdJson()).print()
            ),
            Matchers.contains(
                JsonNodeFactory.instance.objectNode().put("name", name),
                JsonNodeFactory.instance.objectNode().put("name", "")
            )
        );
    }

    /** Rejects identity access before persistence. */
    @Test
    void refusesToInventAnIdentifier() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new BdDraft(new BdInput()).uid(),
            "The unsaved building exposed a database identifier"
        );
    }

    /** Rejects navigation to cabinets before persistence. */
    @Test
    void refusesCabinetAccessBeforeCreation() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new BdDraft(new BdInput()).cabinets(),
            "The unsaved building exposed persisted cabinets"
        );
    }
}
