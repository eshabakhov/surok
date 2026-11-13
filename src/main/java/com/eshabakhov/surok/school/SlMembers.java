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
 * School represented only by its identity and member collection.
 * For example, the permission matrix evaluates ordinary school rights against its members.
 *
 * @since 0.0.1
 */
public final class SlMembers implements School {

    /** Represented school identifier. */
    private final long id;

    /** Represented school members. */
    private final Users members;

    /**
     * New school membership model.
     *
     * @param id Representative school identifier
     * @param members School members
     */
    public SlMembers(final long id, final Users members) {
        this.id = id;
        this.members = members;
    }

    @Override
    public Long uid() {
        return this.id;
    }

    @Override
    public School revise(final SchoolInput input) {
        throw new UnsupportedOperationException("A membership model cannot revise a school");
    }

    @Override
    public Users users() {
        return this.members;
    }

    @Override
    public Buildings buildings() {
        throw new UnsupportedOperationException("A membership model has no buildings");
    }

    @Override
    public Teachers teachers() {
        throw new UnsupportedOperationException("A membership model has no teachers");
    }

    @Override
    public SchoolClasses schoolClasses() {
        throw new UnsupportedOperationException("A membership model has no school classes");
    }

    @Override
    public Subjects subjects() {
        throw new UnsupportedOperationException("A membership model has no subjects");
    }

    @Override
    public Schedules schedules() {
        throw new UnsupportedOperationException("A membership model has no schedules");
    }

    @Override
    public <T> Media<T> print(final Media<T> media) {
        return media.with("id", this.id);
    }
}
