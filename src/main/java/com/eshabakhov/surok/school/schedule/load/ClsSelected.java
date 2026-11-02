/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.ResultPage;
import lombok.EqualsAndHashCode;

/**
 * Selected teaching loads.
 *
 * @since 0.0.1
 */
@EqualsAndHashCode
public final class ClsSelected implements ClassLoads {

    /**
     * Complete collection.
     */
    private final ClassLoads origin;

    /**
     * Selected loads.
     */
    private final Iterable<ClassLoad> items;

    /**
     * Selection pagination.
     */
    private final ResultPage page;

    /**
     * New selected loads.
     *
     * @param origin Complete collection
     * @param items Selected loads
     * @param page Selection pagination
     */
    public ClsSelected(
        final ClassLoads origin,
        final Iterable<ClassLoad> items,
        final ResultPage page
    ) {
        this.origin = origin;
        this.items = items;
        this.page = page;
    }

    @Override
    public ClassLoad load(final long id) throws Exception {
        return this.origin.load(id);
    }

    @Override
    public ClassLoads selection(final Page requested) throws Exception {
        return this.origin.selection(requested);
    }

    @Override
    public Iterable<ClassLoad> iterate() {
        return this.items;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return media.with(new PrItems("load", this.items))
            .with("page", this.page);
    }
}
