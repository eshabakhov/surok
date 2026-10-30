/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok;

import com.eshabakhov.surok.federal.subject.Subjects;
import com.eshabakhov.surok.school.SchoolInput;
import com.eshabakhov.surok.school.building.Buildings;
import com.eshabakhov.surok.school.schedule.Schedules;
import com.eshabakhov.surok.school.schoolclass.SchoolClasses;
import com.eshabakhov.surok.school.teacher.Teachers;

/**
 * School domain entity interface.
 *
 * <p>Represents a school entity with its properties and associated
 * collections of domain objects like cabinets, teachers, classes,
 * subjects and schedules.</p>
 *
 * @since 0.0.1
 */
public interface School extends Printable {

    /**
     * Returns a School unique identifier.
     *
     * @return School's ID
     */
    Long uid();

    /**
     * Revises this school, for example with a validated name input.
     *
     * @param input School input
     * @return Revised school
     * @throws Exception If revision fails
     */
    School revise(SchoolInput input) throws Exception;

    /**
     * Return users.
     *
     * @return Users
     */
    Users users();

    /**
     * Returns collection of school's buildings.
     *
     * @return Buildings collection
     */
    Buildings buildings();

    /**
     * Returns collection of school's teachers.
     *
     * @return Teachers collection
     */
    Teachers teachers();

    /**
     * Returns collection of school's classes.
     *
     * @return School classes collection
     */
    SchoolClasses schoolClasses();

    /**
     * Returns collection of school's subjects.
     *
     * @return Subjects collection
     */
    Subjects subjects();

    /**
     * Returns collection of school's schedules.
     *
     * @return Schedules collection
     */
    Schedules schedules();
}
