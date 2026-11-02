/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.hours;

import com.eshabakhov.surok.Media;
import lombok.EqualsAndHashCode;

/**
 * Existing weekly hours with a curriculum proposal used when no load exists.
 * For example, five current hours and a three-hour proposal retain five planned hours.
 * A zero proposal is absent from the representation.
 *
 * @since 0.0.1
 */
@EqualsAndHashCode
public final class WhProposed implements WeeklyHours {

    /** Existing weekly hours. */
    private final WeeklyHours current;

    /** Curriculum proposal, with zero representing no proposal. */
    private final WeeklyHours proposed;

    /**
     * New weekly hours with a curriculum proposal.
     *
     * @param current Existing weekly hours
     * @param proposed Curriculum proposal
     */
    public WhProposed(final WeeklyHours current, final WeeklyHours proposed) {
        this.current = current;
        this.proposed = proposed;
    }

    @Override
    public int value() {
        final int before = this.current.value();
        final int suggested = this.proposed.value();
        final int result;
        if (before == 0) {
            result = suggested;
        } else {
            result = before;
        }
        return result;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) {
        Media<T> result = media.with("current", this.current.value());
        final int suggested = this.proposed.value();
        if (suggested > 0) {
            result = result.with("proposed", suggested);
        }
        return result.with("planned", this.value());
    }
}
