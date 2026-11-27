/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schoolclass;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.filter.FlWeb;
import com.eshabakhov.surok.sort.StWeb;

/**
 * Browser representation of classes.
 *
 * @since 0.0.1
 */
public final class ScsWeb implements SchoolClasses {

    /** Origin classes. */
    private final SchoolClasses origin;

    /** Applied filters. */
    private final Filters filters;

    /** Applied sorts. */
    private final Sorts sorts;

    /**
     * New browser representation.
     *
     * @param origin Origin classes
     * @param filters Applied filters
     * @param sorts Applied sorts
     */
    public ScsWeb(
        final SchoolClasses origin,
        final Filters filters,
        final Sorts sorts
    ) {
        this.origin = origin;
        this.filters = filters;
        this.sorts = sorts;
    }

    @Override
    public SchoolClass create(final SchoolClassInput input) throws Exception {
        return this.origin.create(input);
    }

    @Override
    public SchoolClass clazz(final long id) throws Exception {
        return this.origin.clazz(id);
    }

    @Override
    public SchoolClasses selection(
        final Filters selected,
        final Page page,
        final Sorts selecteds
    ) throws Exception {
        return this.origin.selection(selected, page, selecteds);
    }

    @Override
    public Iterable<SchoolClass> iterate() {
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
                "filter",
                this.filters.filters().stream().map(FlWeb::new).toList()
            )
        ).with(
            "sorts",
            new PrItems(
                "sort",
                this.sorts.sorts().stream().map(StWeb::new).toList()
            )
        );
    }
}
