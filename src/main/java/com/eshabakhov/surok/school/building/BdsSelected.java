/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.building;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.ResultPage;
import com.eshabakhov.surok.Sorts;
import lombok.EqualsAndHashCode;

/**
 * Selected school buildings.
 *
 * @since 0.0.1
 */
@EqualsAndHashCode
public final class BdsSelected implements Buildings {

    /**
     * Complete collection.
     */
    private final Buildings origin;

    /**
     * Selected buildings.
     */
    private final Iterable<Building> items;

    /**
     * Selection pagination.
     */
    private final ResultPage page;

    /**
     * New selected buildings.
     *
     * @param origin Complete collection
     * @param items Selected buildings
     * @param page Selection pagination
     */
    public BdsSelected(
        final Buildings origin,
        final Iterable<Building> items,
        final ResultPage page
    ) {
        this.origin = origin;
        this.items = items;
        this.page = page;
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
        final Filters filters,
        final Page requested,
        final Sorts sorts
    ) throws Exception {
        return this.origin.selection(filters, requested, sorts);
    }

    @Override
    public Iterable<Building> iterate() {
        return this.items;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return media.with(new PrItems("building", this.items))
            .with(this.page);
    }

    @Override
    public void remove(final long id) throws Exception {
        this.origin.remove(id);
    }
}
