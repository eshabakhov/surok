/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.filter.FlWeb;
import com.eshabakhov.surok.sort.StWeb;

/**
 * Browser representation of curriculum requirements.
 *
 * @since 0.0.1
 */
public final class RqsWeb implements Requirements {

    /** Origin requirements. */
    private final Requirements origin;

    /** Applied filters. */
    private final Filters filters;

    /** Applied sorts. */
    private final Sorts sorts;

    /**
     * New browser representation.
     *
     * @param origin Origin requirements
     * @param filters Applied filters
     * @param sorts Applied sorts
     */
    public RqsWeb(
        final Requirements origin,
        final Filters filters,
        final Sorts sorts
    ) {
        this.origin = origin;
        this.filters = filters;
        this.sorts = sorts;
    }

    @Override
    public Iterable<Requirement> optional(final int grade) throws Exception {
        return this.origin.optional(grade);
    }

    @Override
    public void check(final int grade, final int hours) throws Exception {
        this.origin.check(grade, hours);
    }

    @Override
    public Requirement create(final RequirementInput input) throws Exception {
        return this.origin.create(input);
    }

    @Override
    public Requirement requirement(final long id) throws Exception {
        return this.origin.requirement(id);
    }

    @Override
    public Requirements selection(
        final Filters selected,
        final Page page,
        final Sorts ordered
    ) throws Exception {
        return this.origin.selection(selected, page, ordered);
    }

    @Override
    public Iterable<Requirement> iterate() {
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
