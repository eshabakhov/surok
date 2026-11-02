/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement.input;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.federal.curriculum.requirement.RequirementInput;

/**
 * Federal curriculum requirement input with weekly hours.
 *
 * @since 0.0.1
 */
public final class RqInputHours implements RequirementInput {

    /** Origin input. */
    private final RequirementInput origin;

    /** Weekly hours. */
    private final String hours;

    /**
     * New weekly-hours input.
     *
     * @param origin Origin input
     * @param hours Weekly hours
     */
    public RqInputHours(final RequirementInput origin, final String hours) {
        this.origin = origin;
        this.hours = hours;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        if (this.hours == null || this.hours.isBlank()) {
            throw new RequirementInputException(
                "Requirement weekly hours are required"
            );
        }
        try {
            final int value = Integer.parseInt(this.hours.trim());
            if (value < 0) {
                throw new RequirementInputException("Requirement hours must not be negative");
            }
            return this.origin.print(media).with("hours", value);
        } catch (final NumberFormatException exception) {
            throw new RequirementInputException(
                String.format(
                    "Invalid requirement weekly hours `%s`", this.hours
                ),
                exception
            );
        }
    }
}
