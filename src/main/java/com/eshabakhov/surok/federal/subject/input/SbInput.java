/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject.input;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.federal.subject.SubjectInput;

/**
 * Empty school subject input.
 *
 * @since 0.0.1
 */
public final class SbInput implements SubjectInput {

    /**
     * New empty input.
     */
    public SbInput() {
        // Intentionally empty.
    }

    @Override
    public <T> Media<T> print(final Media<T> media) {
        return media;
    }
}
