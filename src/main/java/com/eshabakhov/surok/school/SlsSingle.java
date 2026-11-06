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
import java.util.List;

/**
 * Schools representation containing one selected school.
 *
 * @since 0.0.1
 */
public final class SlsSingle implements Schools {

    /** Origin schools. */
    private final Schools origin;

    /** Selected school. */
    private final School selected;

    /**
     * New single-school representation.
     *
     * @param origin Origin schools
     * @param selected Selected school
     */
    public SlsSingle(final Schools origin, final School selected) {
        this.origin = origin;
        this.selected = selected;
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
        final Filters filters,
        final Page page,
        final Sorts sorts
    ) throws Exception {
        return this.origin.selection(filters, page, sorts);
    }

    @Override
    public Iterable<School> iterate() {
        return List.of(this.selected);
    }

    @Override
    public void remove(final long id) throws Exception {
        this.origin.remove(id);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return media.with("href", "/schools").with("school", this.selected);
    }
}
