/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.teacher;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.Sorts;
import com.eshabakhov.surok.filter.FlWeb;
import com.eshabakhov.surok.sort.StWeb;

/**
 * Browser representation of teachers.
 *
 * @since 0.0.1
 */
public final class ThsWeb implements Teachers {

    /** Origin teachers. */
    private final Teachers origin;

    /** Applied filters. */
    private final Filters filters;

    /** Applied sorts. */
    private final Sorts sorts;

    /**
     * New browser representation.
     *
     * @param origin Origin teachers
     * @param filters Applied filters
     * @param sorts Applied sorts
     */
    public ThsWeb(
        final Teachers origin,
        final Filters filters,
        final Sorts sorts
    ) {
        this.origin = origin;
        this.filters = filters;
        this.sorts = sorts;
    }

    @Override
    public Teacher create(final TeacherInput input) throws Exception {
        return this.origin.create(input);
    }

    @Override
    public Teacher teacher(final long id) throws Exception {
        return this.origin.teacher(id);
    }

    @Override
    public Teachers selection(
        final Filters selected,
        final Page page,
        final Sorts selecteds
    ) throws Exception {
        return this.origin.selection(selected, page, selecteds);
    }

    @Override
    public Iterable<Teacher> iterate() {
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
