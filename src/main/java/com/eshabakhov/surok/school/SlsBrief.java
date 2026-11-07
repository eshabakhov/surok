/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.Schools;
import com.eshabakhov.surok.Sorts;

/**
 * Browser representation of schools.
 *
 * @since 0.0.1
 */
public final class SlsBrief implements Schools {

    /** Origin schools. */
    private final Schools origin;

    /**
     * New navigation representation.
     *
     * @param origin Origin schools
     */
    public SlsBrief(final Schools origin) {
        this.origin = origin;
    }

    @Override
    public boolean contains(final long id) {
        return this.origin.contains(id);
    }

    @Override
    public School create(final SchoolInput input) throws Exception {
        return this.origin.create(input);
    }

    @Override
    public School school(final long id) throws Exception {
        return this.origin.school(id);
    }

    @Override
    public Schools selection(
        final Filters selected,
        final Page page,
        final Sorts selecteds
    ) throws Exception {
        return this.origin.selection(selected, page, selecteds);
    }

    @Override
    public Iterable<School> iterate() {
        return this.origin.iterate();
    }

    @Override
    public void remove(final long id) throws Exception {
        this.origin.remove(id);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return media.with("href", "/schools");
    }
}
