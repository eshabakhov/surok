/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.ResultPage;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.Schools;
import com.eshabakhov.surok.Sorts;
import lombok.EqualsAndHashCode;

/**
 * Selected schools.
 *
 * @since 0.0.1
 */
@EqualsAndHashCode
public final class SlsSelected implements Schools {

    /** Complete collection. */
    private final Schools origin;

    /** Selected schools. */
    private final Iterable<School> items;

    /** Selection pagination. */
    private final ResultPage page;

    /**
     * New selected schools.
     *
     * @param origin Complete collection
     * @param items Selected schools
     * @param page Selection pagination
     */
    public SlsSelected(
        final Schools origin,
        final Iterable<School> items,
        final ResultPage page
    ) {
        this.origin = origin;
        this.items = items;
        this.page = page;
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
        final Page requested,
        final Sorts sorts
    ) throws Exception {
        return this.origin.selection(filters, requested, sorts);
    }

    @Override
    public Iterable<School> iterate() {
        return this.items;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return media.with(new PrItems("school", this.items))
            .with("page", this.page);
    }

    @Override
    public void remove(final long id) throws Exception {
        this.origin.remove(id);
    }
}
