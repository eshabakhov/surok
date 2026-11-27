/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schoolclass;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.ResultPage;
import com.eshabakhov.surok.Sorts;
import lombok.EqualsAndHashCode;

/**
 * Selected school classes.
 *
 * @since 0.0.1
 */
@EqualsAndHashCode
public final class ScsSelected implements SchoolClasses {

    /**
     * Complete collection.
     */
    private final SchoolClasses origin;

    /**
     * Selected classes.
     */
    private final Iterable<SchoolClass> items;

    /**
     * Selection pagination.
     */
    private final ResultPage page;

    /**
     * New selected classes.
     *
     * @param origin Complete collection
     * @param items Selected classes
     * @param page Selection pagination
     */
    public ScsSelected(
        final SchoolClasses origin,
        final Iterable<SchoolClass> items,
        final ResultPage page
    ) {
        this.origin = origin;
        this.items = items;
        this.page = page;
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
        final Filters filters,
        final Page requested,
        final Sorts sorts
    ) throws Exception {
        return this.origin.selection(filters, requested, sorts);
    }

    @Override
    public Iterable<SchoolClass> iterate() {
        return this.items;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return media.with(new PrItems("school-class", this.items))
            .with(this.page);
    }

    @Override
    public void remove(final long id) throws Exception {
        this.origin.remove(id);
    }
}
