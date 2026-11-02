/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.hours;

import com.eshabakhov.surok.Printable;

/**
 * Weekly teaching hours with a nonnegative value and a printable representation.
 * For example, a calculated value can retain current hours and a curriculum proposal.
 * Zero represents the absence or removal of a teaching load.
 *
 * @since 0.0.1
 */
public interface WeeklyHours extends Printable {

    /**
     * Weekly hours for calculation, display, and application.
     *
     * @return Nonnegative weekly hours
     */
    int value();
}
