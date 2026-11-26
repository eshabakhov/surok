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
 * Unsaved school supplying the fields of a creation form.
 * For example, {@code draft.revise(input)} returns new fields without writing to storage.
 *
 * @since 0.0.1
 */
public final class SlDraft implements School {

    /** Fields entered before creation. */
    private final SchoolInput input;

    /**
     * New unsaved school.
     *
     * @param input Fields entered before creation
     */
    public SlDraft(final SchoolInput input) {
        this.input = input;
    }

    @Override
    public Long uid() {
        throw new IllegalStateException("An unsaved school has no identifier");
    }

    @Override
    public School revise(final SchoolInput fields) {
        return new SlDraft(fields);
    }

    @Override
    public Users users() {
        throw new IllegalStateException("An unsaved school has no users");
    }

    @Override
    public Buildings buildings() {
        throw new IllegalStateException("An unsaved school has no buildings");
    }

    @Override
    public Teachers teachers() {
        throw new IllegalStateException("An unsaved school has no teachers");
    }

    @Override
    public SchoolClasses schoolClasses() {
        throw new IllegalStateException("An unsaved school has no school classes");
    }

    @Override
    public Subjects subjects() {
        throw new IllegalStateException("An unsaved school has no subjects");
    }

    @Override
    public Schedules schedules() {
        throw new IllegalStateException("An unsaved school has no schedules");
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.input.print(media.with("name", ""));
    }
}
