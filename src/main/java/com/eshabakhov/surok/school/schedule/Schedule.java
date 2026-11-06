/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule;

import com.eshabakhov.surok.Printable;
import com.eshabakhov.surok.school.schedule.load.ClassLoads;
import com.eshabakhov.surok.school.schedule.load.autofill.LoadDraft;

/**
 * School schedule printed into a supplied medium.
 *
 * @since 0.0.1
 */
public interface Schedule extends Printable {

    /**
     * Schedule identifier.
     *
     * @return Identifier
     */
    Long uid();

    /**
     * Revises the schedule using the supplied input.
     *
     * @param input Schedule input
     * @return Revised schedule
     * @throws Exception If revision fails
     */
    Schedule revise(ScheduleInput input) throws Exception;

    /**
     * Teaching loads belonging to this schedule.
     *
     * @return Teaching loads
     */
    ClassLoads loads();

    /**
     * Planning draft belonging to this schedule.
     *
     * @return Teaching load draft
     */
    LoadDraft draft();
}
