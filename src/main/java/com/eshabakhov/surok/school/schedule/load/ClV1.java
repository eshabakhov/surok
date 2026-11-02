/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load;

import com.eshabakhov.surok.Media;

/**
 * Version 1 implementation of {@link ClassLoad}.
 *
 * @since 0.0.1
 */
public final class ClV1 implements ClassLoad {

    /**
     * Original load.
     */
    private final ClassLoad origin;

    /**
     * Creates a version 1 load.
     *
     * @param origin Original load
     */
    public ClV1(final ClassLoad origin) {
        this.origin = origin;
    }

    @Override
    public Long uid() {
        return this.origin.uid();
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.origin.print(media).include(
            "id", "schoolClass", "subject", "hours"
        );
    }
}
