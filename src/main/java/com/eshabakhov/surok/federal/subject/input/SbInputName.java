/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject.input;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.federal.subject.SubjectInput;

/**
 * School subject input with a name.
 *
 * @since 0.0.1
 */
public final class SbInputName implements SubjectInput {

    /** Origin input. */
    private final SubjectInput origin;

    /** Subject name. */
    private final String name;

    /**
     * New name input.
     *
     * @param origin Origin input
     * @param name Subject name
     */
    public SbInputName(final SubjectInput origin, final String name) {
        this.origin = origin;
        this.name = name;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        if (this.name == null || this.name.isBlank()) {
            throw new SubjectInputException(
                "Subject name is required"
            );
        }
        return this.origin.print(media).with("name", this.name.trim());
    }
}
