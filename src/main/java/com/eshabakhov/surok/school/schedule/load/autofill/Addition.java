/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.autofill;

import com.eshabakhov.surok.Printable;
import com.eshabakhov.surok.federal.subject.Subject;
import com.eshabakhov.surok.school.schedule.load.hours.WeeklyHours;
import java.util.Map;

/**
 * Additional subjects and their weekly hours for one grade in a draft.
 * For example, saving one hour of robotics proposes that hour for every class in the grade.
 * Mutations participate in the owning draft's locked transaction.
 *
 * @since 0.0.1
 */
public interface Addition extends Printable {

    /**
     * Replaces the selected subjects and checks the resulting grade totals.
     * Omitted subjects retain zero hours until their actual loads are removed.
     *
     * @param hours Selected subjects and weekly hours
     * @throws Exception If subjects are unavailable or the maximum is exceeded
     */
    void save(Map<Subject, WeeklyHours> hours) throws Exception;

    /**
     * Checks subject availability and totals including individual class loads.
     *
     * @throws Exception If a subject or total no longer meets the requirements
     */
    void check() throws Exception;
}
