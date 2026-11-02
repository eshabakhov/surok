/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.ResultPage;
import com.eshabakhov.surok.Sorts;
import lombok.EqualsAndHashCode;

/**
 * Selected federal curriculum requirements.
 *
 * @since 0.0.1
 */
@EqualsAndHashCode
public final class RqsSelected implements Requirements {

    /**
     * Complete collection.
     */
    private final Requirements origin;

    /**
     * Selected requirements.
     */
    private final Iterable<Requirement> items;

    /**
     * Selection pagination.
     */
    private final ResultPage page;

    /**
     * New selected requirements.
     *
     * @param origin Complete collection
     * @param items Selected requirements
     * @param page Selection pagination
     */
    public RqsSelected(
        final Requirements origin,
        final Iterable<Requirement> items,
        final ResultPage page
    ) {
        this.origin = origin;
        this.items = items;
        this.page = page;
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
        final Filters filters,
        final Page requested,
        final Sorts sorts
    ) throws Exception {
        return this.origin.selection(filters, requested, sorts);
    }

    @Override
    public Iterable<Requirement> iterate() {
        return this.items;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return media.with(new PrItems("requirement", this.items))
            .with("page", this.page);
    }

    @Override
    public void remove(final long id) throws Exception {
        this.origin.remove(id);
    }
}
