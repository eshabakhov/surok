/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.autofill;

import com.eshabakhov.surok.Printable;
import com.eshabakhov.surok.federal.subject.Subject;
import com.eshabakhov.surok.school.schedule.load.hours.WeeklyHours;
import com.eshabakhov.surok.school.schoolclass.SchoolClass;

/**
 * A class's planned weekly load for a school subject.
 * Decisions return new objects without changing the original or writing to a database.
 *
 * @since 0.0.1
 */
public interface PlannedLoad extends Printable {

    /**
     * Class whose load is being planned.
     *
     * @return School class
     */
    SchoolClass clazz();

    /**
     * Subject whose load is being planned.
     *
     * @return School subject
     */
    Subject subject();

    /**
     * Weekly hours retaining the current load, proposal, and planned result.
     *
     * @return Printable weekly hours
     */
    WeeklyHours hours();

    /**
     * Returns a load with manually specified hours.
     * Zero marks the load for removal.
     *
     * @param hours Weekly teaching hours
     * @return Changed load
     * @throws Exception If the hours are invalid
     */
    PlannedLoad with(WeeklyHours hours) throws Exception;

    /**
     * Returns a load accepting the curriculum's proposed hours.
     *
     * @return Changed load
     * @throws Exception If no applicable proposal exists
     */
    PlannedLoad accept() throws Exception;

    /**
     * Applies this load to a typed destination within an application transaction.
     * Initial existing loads are preserved; missing proposals and explicit choices are written.
     *
     * @param target Destination for actual teaching loads
     * @throws Exception If the load cannot be applied
     */
    void apply(LoadTarget target) throws Exception;
}
