/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.autofill;

import com.eshabakhov.surok.federal.subject.Subject;
import com.eshabakhov.surok.school.schedule.load.hours.WeeklyHours;
import com.eshabakhov.surok.school.schoolclass.SchoolClass;

/**
 * Typed destination for actual teaching loads inside an application transaction.
 * For example, {@code load.apply(target)} writes chosen hours without parsing a representation.
 * Zero hours remove the record rather than creating a zero-hour load.
 *
 * @since 0.0.1
 */
@FunctionalInterface
public interface LoadTarget {

    /**
     * Writes the requested hours for one selected class and subject.
     *
     * @param clazz Selected school class
     * @param subject School subject
     * @param hours Positive hours to save, or zero to remove
     * @throws Exception If the load cannot be written
     */
    void write(SchoolClass clazz, Subject subject, WeeklyHours hours) throws Exception;
}
