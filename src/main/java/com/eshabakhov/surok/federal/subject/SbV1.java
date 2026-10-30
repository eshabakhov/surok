/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

import com.eshabakhov.surok.Media;

/**
 * Version 1 implementation of {@link Subject}.
 *
 * @since 0.0.1
 */
public final class SbV1 implements Subject {

    /**
     * Original subject.
     */
    private final Subject origin;

    /**
     * Creates a version 1 subject.
     *
     * @param origin Original subject
     */
    public SbV1(final Subject origin) {
        this.origin = origin;
    }

    @Override
    public Long uid() {
        return this.origin.uid();
    }

    @Override
    public String name() throws Exception {
        return this.origin.name();
    }

    @Override
    public Origin origin() throws Exception {
        return this.origin.origin();
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.origin.print(media).include(
            "id", "name", "origin"
        );
    }

    @Override
    public Subject revise(final SubjectInput input) throws Exception {
        return this.origin.revise(input);
    }
}
