/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.autofill;

import com.eshabakhov.surok.Printable;
import com.eshabakhov.surok.federal.subject.Subject;
import com.eshabakhov.surok.school.schedule.load.hours.WeeklyHours;
import java.util.Map;

/**
 * Additional subjects for the grades of a school schedule's curriculum.
 * For example, iteration exposes each grade represented by active applicable classes once.
 * Mutations participate in the owning draft's locked transaction.
 *
 * @since 0.0.1
 */
public interface Additions extends Printable {

    /**
     * Reads additions for grades represented by active applicable school classes.
     *
     * @return Grade additions in ascending order
     * @throws Exception If reading fails
     */
    Iterable<Addition> iterate() throws Exception;

    /**
     * Replaces grade selections, including clearing omitted saved selections.
     *
     * @param additions Selected subjects and hours per grade
     * @throws Exception If a grade, subject or total is invalid
     */
    void save(Map<Integer, Map<Subject, WeeklyHours>> additions) throws Exception;
}
