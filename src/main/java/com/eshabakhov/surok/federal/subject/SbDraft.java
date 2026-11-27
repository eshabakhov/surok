/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.federal.subject.input.MdSbInput;

/**
 * Unsaved subject supplying the fields of a creation form.
 * For example, {@code draft.revise(input)} returns new fields without writing to storage.
 *
 * @since 0.0.1
 */
public final class SbDraft implements Subject {

    /** Fields entered before creation. */
    private final SubjectInput input;

    /** Catalogue owning the future subject. */
    private final Origin origin;

    /**
     * New unsaved subject.
     *
     * @param input Fields entered before creation
     * @param origin Catalogue owning the future subject
     */
    public SbDraft(final SubjectInput input, final Origin origin) {
        this.input = input;
        this.origin = origin;
    }

    @Override
    public Long uid() {
        throw new IllegalStateException("An unsaved subject has no identifier");
    }

    @Override
    public Subject revise(final SubjectInput fields) {
        return new SbDraft(fields, this.origin);
    }

    @Override
    public String name() throws Exception {
        return this.input.print(new MdSbInput()).print().getOrDefault("name", "");
    }

    @Override
    public Origin origin() {
        return this.origin;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.input.print(media.with("name", "").with("origin", this.origin.name()));
    }
}
