/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.federal.curriculum.requirement.Requirements;
import java.util.List;

/**
 * Curriculum representation with its requirements.
 *
 * @since 0.0.1
 */
public final class CrWithRequirements implements Curriculum {

    /** Origin curriculum. */
    private final Curriculum origin;

    /** Curriculum requirements. */
    private final Requirements requirements;

    /**
     * New curriculum representation.
     *
     * @param origin Origin curriculum
     * @param requirements Curriculum requirements
     */
    public CrWithRequirements(
        final Curriculum origin,
        final Requirements requirements
    ) {
        this.origin = origin;
        this.requirements = requirements;
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
    public Curriculum revise(final CurriculumInput input) throws Exception {
        return this.origin.revise(input);
    }

    @Override
    public Requirements requirements() {
        return this.requirements;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.origin.print(media).with(
            "requirements", this.requirements
        );
    }
}
