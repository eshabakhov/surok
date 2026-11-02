/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.filter.FlWeb;
import com.eshabakhov.surok.sort.StWeb;

/**
 * Browser representation of subjects.
 *
 * @since 0.0.1
 */
public final class SbsWeb implements Subjects {

    /** Origin subjects. */
    private final Subjects origin;

    /** Applied filters. */
    private final Filters filters;

    /** Applied sorts. */
    private final Sorts sorts;

    /**
     * New browser representation.
     *
     * @param origin Origin subjects
     * @param filters Applied filters
     * @param sorts Applied sorts
     */
    public SbsWeb(
        final Subjects origin,
        final Filters filters,
        final Sorts sorts
    ) {
        this.origin = origin;
        this.filters = filters;
        this.sorts = sorts;
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
        final Filters selected,
        final Page page,
        final Sorts selecteds
    ) throws Exception {
        return this.origin.selection(selected, page, selecteds);
    }

    @Override
    public Iterable<Subject> iterate() {
        return this.origin.iterate();
    }

    @Override
    public void remove(final long id) throws Exception {
        this.origin.remove(id);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return this.origin.print(media).with(
            "filters",
            new PrItems(
                "item",
                this.filters.filters().stream().map(FlWeb::new).toList()
            )
        ).with(
            "sorts",
            new PrItems(
                "item",
                this.sorts.sorts().stream().map(StWeb::new).toList()
            )
        );
    }
}
