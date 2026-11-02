/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.ResultPage;
import com.eshabakhov.surok.Sorts;
import lombok.EqualsAndHashCode;

/**
 * Selected school subjects.
 *
 * @since 0.0.1
 */
@EqualsAndHashCode
public final class SbsSelected implements Subjects {

    /**
     * Complete collection.
     */
    private final Subjects origin;

    /**
     * Selected subjects.
     */
    private final Iterable<Subject> items;

    /**
     * Selection pagination.
     */
    private final ResultPage page;

    /**
     * New selected subjects.
     *
     * @param origin Complete collection
     * @param items Selected subjects
     * @param page Selection pagination
     */
    public SbsSelected(
        final Subjects origin,
        final Iterable<Subject> items,
        final ResultPage page
    ) {
        this.origin = origin;
        this.items = items;
        this.page = page;
    }

    @Override
    public Subject create(final SubjectInput input) throws Exception {
        return this.origin.create(input);
    }

    @Override
    public Subject subject(final long id) throws Exception {
        return this.origin.subject(id);
    }

    @Override
    public Subjects selection(
        final Filters filters,
        final Page requested,
        final Sorts sorts
    ) throws Exception {
        return this.origin.selection(filters, requested, sorts);
    }

    @Override
    public Iterable<Subject> iterate() {
        return this.items;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return media.with(new PrItems("subject", this.items))
            .with("page", this.page);
    }

    @Override
    public void remove(final long id) throws Exception {
        this.origin.remove(id);
    }
}
