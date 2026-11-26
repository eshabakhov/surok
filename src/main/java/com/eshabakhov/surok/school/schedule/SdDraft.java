/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.school.schedule.load.ClassLoads;
import com.eshabakhov.surok.school.schedule.load.autofill.LoadDraft;

/**
 * Unsaved schedule supplying the fields of a creation form.
 * For example, {@code draft.revise(input)} returns new fields without writing to storage.
 *
 * @since 0.0.1
 */
public final class SdDraft implements Schedule {

    /** Fields entered before creation. */
    private final ScheduleInput input;

    /**
     * New unsaved schedule.
     *
     * @param input Fields entered before creation
     */
    public SdDraft(final ScheduleInput input) {
        this.input = input;
    }

    @Override
    public Long uid() {
        throw new IllegalStateException("An unsaved schedule has no identifier");
    }

    @Override
    public Schedule revise(final ScheduleInput fields) {
        return new SdDraft(fields);
    }

    @Override
    public ClassLoads loads() {
        throw new IllegalStateException("An unsaved schedule has no loads");
    }

    @Override
    public LoadDraft draft() {
        throw new IllegalStateException("An unsaved schedule has no draft");
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.input.print(media.with("name", ""));
    }
}
