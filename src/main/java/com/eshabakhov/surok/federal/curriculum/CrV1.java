/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.federal.curriculum.requirement.Requirements;
import java.util.List;

/**
 * Version 1 implementation of {@link Curriculum}.
 *
 * @since 0.0.1
 */
public final class CrV1 implements Curriculum {

    /**
     * Original federal curriculum.
     */
    private final Curriculum origin;

    /**
     * Creates a version 1 curriculum.
     *
     * @param origin Original federal curriculum
     */
    public CrV1(final Curriculum origin) {
        this.origin = origin;
    }

    @Override
    public List<Integer> grades() throws Exception {
        return this.origin.grades();
    }

    @Override
    public Long uid() {
        return this.origin.uid();
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.origin.print(media).include(
            "id", "title", "level", "week", "version", "year"
        );
    }

    @Override
    public Curriculum revise(final CurriculumInput input) throws Exception {
        return this.origin.revise(input);
    }

    @Override
    public Requirements requirements() {
        return this.origin.requirements();
    }
}
