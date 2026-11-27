/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.building.cabinet;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.ResultPage;
import com.eshabakhov.surok.Sorts;
import lombok.EqualsAndHashCode;

/**
 * Selected building cabinets.
 *
 * @since 0.0.1
 */
@EqualsAndHashCode
public final class CbsSelected implements Cabinets {

    /**
     * Complete collection.
     */
    private final Cabinets origin;

    /**
     * Selected cabinets.
     */
    private final Iterable<Cabinet> items;

    /**
     * Selection pagination.
     */
    private final ResultPage page;

    /**
     * New selected cabinets.
     *
     * @param origin Complete collection
     * @param items Selected cabinets
     * @param page Selection pagination
     */
    public CbsSelected(
        final Cabinets origin,
        final Iterable<Cabinet> items,
        final ResultPage page
    ) {
        this.origin = origin;
        this.items = items;
        this.page = page;
    }

    @Override
    public Cabinet create(final CabinetInput input) throws Exception {
        return this.origin.create(input);
    }

    @Override
    public Cabinet cabinet(final long id) throws Exception {
        return this.origin.cabinet(id);
    }

    @Override
    public Cabinets selection(
        final Filters filters,
        final Page requested,
        final Sorts sorts
    ) throws Exception {
        return this.origin.selection(filters, requested, sorts);
    }

    @Override
    public Iterable<Cabinet> iterate() {
        return this.items;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return media.with(new PrItems("cabinet", this.items))
            .with(this.page);
    }

    @Override
    public void remove(final long id) throws Exception {
        this.origin.remove(id);
    }
}
