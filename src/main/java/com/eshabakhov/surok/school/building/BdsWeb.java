/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.building;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.filter.FlWeb;
import com.eshabakhov.surok.sort.StWeb;

/**
 * Browser representation of buildings.
 *
 * @since 0.0.1
 */
public final class BdsWeb implements Buildings {

    /** Origin buildings. */
    private final Buildings origin;

    /** Applied filters. */
    private final Filters filters;

    /** Applied sorts. */
    private final Sorts sorts;

    /**
     * New browser representation.
     *
     * @param origin Origin buildings
     * @param filters Applied filters
     * @param sorts Applied sorts
     */
    public BdsWeb(
        final Buildings origin,
        final Filters filters,
        final Sorts sorts
    ) {
        this.origin = origin;
        this.filters = filters;
        this.sorts = sorts;
    }

    @Override
    public Building create(final BuildingInput input) throws Exception {
        return this.origin.create(input);
    }

    @Override
    public Building building(final long id) throws Exception {
        return this.origin.building(id);
    }

    @Override
    public Buildings selection(
        final Filters selected,
        final Page page,
        final Sorts selecteds
    ) throws Exception {
        return this.origin.selection(selected, page, selecteds);
    }

    @Override
    public Iterable<Building> iterate() {
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
