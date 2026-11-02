/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.hours;

import com.eshabakhov.surok.Media;
import lombok.EqualsAndHashCode;

/**
 * Fixed weekly teaching hours, validated when evaluated.
 * For example, {@code new WhFixed(6).value()} evaluates to six hours.
 * Printing adds a numeric hours field without assigning a planning state.
 *
 * @since 0.0.1
 */
@EqualsAndHashCode
public final class WhFixed implements WeeklyHours {

    /** Requested number of hours. */
    private final int hours;

    /**
     * New weekly hours.
     *
     * @param hours Number of hours, with zero representing no load
     */
    public WhFixed(final int hours) {
        this.hours = hours;
    }

    @Override
    public int value() {
        if (this.hours < 0) {
            throw new IllegalArgumentException("Weekly teaching hours must not be negative");
        }
        return this.hours;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) {
        return media.with("hours", this.value());
    }
}
