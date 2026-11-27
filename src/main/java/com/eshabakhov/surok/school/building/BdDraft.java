/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.building;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.school.building.cabinet.Cabinets;

/**
 * Unsaved building whose fields can populate a creation form.
 * For example, {@code new BdDraft(new BdInput())} prints an empty name without an identifier.
 * Revising returns another draft; persistence remains the responsibility of {@link Buildings}.
 *
 * @since 0.0.1
 */
public final class BdDraft implements Building {

    /** Building fields. */
    private final BuildingInput input;

    /**
     * New unsaved building with the supplied fields.
     *
     * @param input Building fields
     */
    public BdDraft(final BuildingInput input) {
        this.input = input;
    }

    @Override
    public Long uid() {
        throw new IllegalStateException("An unsaved building has no identifier");
    }

    @Override
    public Building revise(final BuildingInput fields) {
        return new BdDraft(fields);
    }

    @Override
    public Cabinets cabinets() {
        throw new IllegalStateException("An unsaved building has no cabinets");
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.input.print(media.with("name", ""));
    }
}
