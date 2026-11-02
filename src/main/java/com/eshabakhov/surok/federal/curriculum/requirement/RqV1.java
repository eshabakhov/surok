/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement;

import com.eshabakhov.surok.Media;

/**
 * Version 1 implementation of {@link Requirement}.
 *
 * @since 0.0.1
 */
public final class RqV1 implements Requirement {

    /** Original federal curriculum requirement. */
    private final Requirement origin;

    /**
     * New version 1 requirement.
     *
     * @param origin Original federal curriculum requirement
     */
    public RqV1(final Requirement origin) {
        this.origin = origin;
    }

    @Override
    public Long uid() {
        return this.origin.uid();
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.origin.print(media).include(
            "id", "grade", "subject", "hours", "partType"
        );
    }

    @Override
    public Requirement revise(final RequirementInput input) throws Exception {
        return this.origin.revise(input);
    }
}
