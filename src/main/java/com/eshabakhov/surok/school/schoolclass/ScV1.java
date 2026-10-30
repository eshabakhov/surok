/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schoolclass;

import com.eshabakhov.surok.Media;

/**
 * Version 1 implementation of {@link SchoolClass}.
 *
 * @since 0.0.1
 */
public final class ScV1 implements SchoolClass {

    /**
     * Original class.
     */
    private final SchoolClass origin;

    /**
     * Creates a version 1 class.
     *
     * @param origin Original class
     */
    public ScV1(final SchoolClass origin) {
        this.origin = origin;
    }

    @Override
    public Long uid() {
        return this.origin.uid();
    }

    @Override
    public int grade() throws Exception {
        return this.origin.grade();
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.origin.print(media).include(
            "id", "name", "grade", "litera"
        );
    }

    @Override
    public SchoolClass revise(final SchoolClassInput input) throws Exception {
        return this.origin.revise(input);
    }
}
