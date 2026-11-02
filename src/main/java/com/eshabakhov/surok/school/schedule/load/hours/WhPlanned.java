/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.hours;

import com.eshabakhov.surok.Media;
import lombok.EqualsAndHashCode;

/**
 * Selected weekly hours retaining the original current hours and curriculum proposal.
 * For example, selecting zero hours prints a removal without discarding the original facts.
 *
 * @since 0.0.1
 */
@EqualsAndHashCode
public final class WhPlanned implements WeeklyHours {

    /** Original hours and their context. */
    private final WeeklyHours origin;

    /** Selected hours, with zero representing removal. */
    private final WeeklyHours selected;

    /**
     * New selected weekly hours.
     *
     * @param origin Original hours and proposal
     * @param selected Hours to apply
     */
    public WhPlanned(final WeeklyHours origin, final WeeklyHours selected) {
        this.origin = origin;
        this.selected = selected;
    }

    @Override
    public int value() {
        return this.selected.value();
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.origin.print(media).with("planned", this.value());
    }
}
