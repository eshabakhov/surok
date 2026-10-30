/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.Users;
import com.eshabakhov.surok.federal.subject.Subjects;
import com.eshabakhov.surok.school.building.Buildings;
import com.eshabakhov.surok.school.schedule.Schedules;
import com.eshabakhov.surok.school.schoolclass.SchoolClasses;
import com.eshabakhov.surok.school.teacher.Teachers;

/**
 * Versioned school fields, for example {@code new SlV1(school).print(media)}.
 *
 * @since 0.0.1
 */
public final class SlV1 implements School {

    /** Original school. */
    private final School origin;

    /**
     * New versioned school, for example {@code new SlV1(school)}.
     *
     * @param origin Original school
     */
    public SlV1(final School origin) {
        this.origin = origin;
    }

    @Override
    public Long uid() {
        return this.origin.uid();
    }

    @Override
    public School revise(final SchoolInput input) throws Exception {
        return this.origin.revise(input);
    }

    @Override
    public Users users() {
        return this.origin.users();
    }

    @Override
    public Buildings buildings() {
        return this.origin.buildings();
    }

    @Override
    public Teachers teachers() {
        return this.origin.teachers();
    }

    @Override
    public SchoolClasses schoolClasses() {
        return this.origin.schoolClasses();
    }

    @Override
    public Subjects subjects() {
        return this.origin.subjects();
    }

    @Override
    public Schedules schedules() {
        return this.origin.schedules();
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.origin.print(media).include("id", "name");
    }
}

