/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement.input;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.federal.curriculum.requirement.RequirementInput;

/**
 * Federal curriculum requirement input with a subject.
 *
 * @since 0.0.1
 */
public final class RqInputSubject implements RequirementInput {

    /** Origin input. */
    private final RequirementInput origin;

    /** Requirement subject. */
    private final String subject;

    /**
     * New subject input.
     *
     * @param origin Origin input
     * @param subject Requirement subject
     */
    public RqInputSubject(
        final RequirementInput origin,
        final String subject
    ) {
        this.origin = origin;
        this.subject = subject;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        if (this.subject == null || this.subject.isBlank()) {
            throw new RequirementInputException(
                "Requirement subject is required"
            );
        }
        try {
            if (Long.parseLong(this.subject.trim()) <= 0) {
                throw new RequirementInputException("Subject identifier must be positive");
            }
        } catch (final NumberFormatException exception) {
            throw new RequirementInputException("Subject identifier must be an integer", exception);
        }
        return this.origin.print(media).with(
            "subjectId", this.subject.trim()
        );
    }
}
