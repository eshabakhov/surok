/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import java.util.List;

/**
 * Load collection representation containing one selected load.
 *
 * @since 0.0.1
 */
public final class ClsSingle implements ClassLoads {

    /** Origin loads. */
    private final ClassLoads origin;

    /** Selected load. */
    private final ClassLoad selected;

    /**
     * New representation containing one teaching load.
     *
     * @param origin Origin loads
     * @param selected Selected load
     */
    public ClsSingle(final ClassLoads origin, final ClassLoad selected) {
        this.origin = origin;
        this.selected = selected;
    }

    @Override
    public ClassLoad load(final long id) throws Exception {
        return this.origin.load(id);
    }

    @Override
    public ClassLoads selection(final Page page) throws Exception {
        return this.origin.selection(page);
    }

    @Override
    public Iterable<ClassLoad> iterate() {
        return List.of(this.selected);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return media.with("load", this.selected);
    }
}
