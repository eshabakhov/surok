/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.school.schedule.load.ClassLoads;
import com.eshabakhov.surok.school.schedule.load.autofill.LoadDraft;

/**
 * Version 1 implementation of {@link Schedule}.
 *
 * @since 0.0.1
 */
public final class SdV1 implements Schedule {

    /**
     * Original schedule.
     */
    private final Schedule origin;

    /**
     * Creates a version 1 schedule.
     *
     * @param origin Original schedule
     */
    public SdV1(final Schedule origin) {
        this.origin = origin;
    }

    @Override
    public Long uid() {
        return this.origin.uid();
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.origin.print(media).include(
            "id", "name"
        );
    }

    @Override
    public Schedule revise(final ScheduleInput input) throws Exception {
        return this.origin.revise(input);
    }

    @Override
    public ClassLoads loads() {
        return this.origin.loads();
    }

    @Override
    public LoadDraft draft() {
        return this.origin.draft();
    }
}
