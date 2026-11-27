/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.building.cabinet;

import com.eshabakhov.surok.Media;

/**
 * Unsaved cabinet supplying the fields of a creation form.
 * For example, {@code draft.revise(input)} returns new fields without writing to storage.
 *
 * @since 0.0.1
 */
public final class CbDraft implements Cabinet {

    /** Fields entered before creation. */
    private final CabinetInput input;

    /**
     * New unsaved cabinet.
     *
     * @param input Fields entered before creation
     */
    public CbDraft(final CabinetInput input) {
        this.input = input;
    }

    @Override
    public Long uid() {
        throw new IllegalStateException("An unsaved cabinet has no identifier");
    }

    @Override
    public Cabinet revise(final CabinetInput fields) {
        return new CbDraft(fields);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.input.print(media.with("name", ""));
    }
}
