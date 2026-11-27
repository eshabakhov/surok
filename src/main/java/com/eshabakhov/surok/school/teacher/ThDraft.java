/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.teacher;

import com.eshabakhov.surok.Media;

/**
 * Unsaved teacher supplying the fields of a creation form.
 * For example, {@code draft.revise(input)} returns new fields without writing to storage.
 *
 * @since 0.0.1
 */
public final class ThDraft implements Teacher {

    /** Fields entered before creation. */
    private final TeacherInput input;

    /**
     * New unsaved teacher.
     *
     * @param input Fields entered before creation
     */
    public ThDraft(final TeacherInput input) {
        this.input = input;
    }

    @Override
    public Long uid() {
        throw new IllegalStateException("An unsaved teacher has no identifier");
    }

    @Override
    public Teacher revise(final TeacherInput fields) {
        return new ThDraft(fields);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.input.print(media.with("name", ""));
    }
}
