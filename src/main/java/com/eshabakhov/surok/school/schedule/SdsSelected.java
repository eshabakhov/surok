/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.ResultPage;
import com.eshabakhov.surok.Sorts;
import lombok.EqualsAndHashCode;

/**
 * Selected school schedules.
 *
 * @since 0.0.1
 */
@EqualsAndHashCode
public final class SdsSelected implements Schedules {

    /**
     * Complete collection.
     */
    private final Schedules origin;

    /**
     * Selected schedules.
     */
    private final Iterable<Schedule> items;

    /**
     * Selection pagination.
     */
    private final ResultPage page;

    /**
     * New selected schedules.
     *
     * @param origin Complete collection
     * @param items Selected schedules
     * @param page Selection pagination
     */
    public SdsSelected(
        final Schedules origin,
        final Iterable<Schedule> items,
        final ResultPage page
    ) {
        this.origin = origin;
        this.items = items;
        this.page = page;
    }

    @Override
    public Schedule create(final ScheduleInput input) throws Exception {
        return this.origin.create(input);
    }

    @Override
    public Schedule schedule(final long id) throws Exception {
        return this.origin.schedule(id);
    }

    @Override
    public Schedules selection(
        final Filters filters,
        final Page requested,
        final Sorts sorts
    ) throws Exception {
        return this.origin.selection(filters, requested, sorts);
    }

    @Override
    public Iterable<Schedule> iterate() {
        return this.items;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return media.with(new PrItems("schedule", this.items))
            .with(this.page);
    }

    @Override
    public void remove(final long id) throws Exception {
        this.origin.remove(id);
    }
}
