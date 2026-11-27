/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.teacher;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.ResultPage;
import com.eshabakhov.surok.Sorts;
import lombok.EqualsAndHashCode;

/**
 * Selected school teachers.
 *
 * @since 0.0.1
 */
@EqualsAndHashCode
public final class ThsSelected implements Teachers {

    /**
     * Complete collection.
     */
    private final Teachers origin;

    /**
     * Selected teachers.
     */
    private final Iterable<Teacher> items;

    /**
     * Selection pagination.
     */
    private final ResultPage page;

    /**
     * New selected teachers.
     *
     * @param origin Complete collection
     * @param items Selected teachers
     * @param page Selection pagination
     */
    public ThsSelected(
        final Teachers origin,
        final Iterable<Teacher> items,
        final ResultPage page
    ) {
        this.origin = origin;
        this.items = items;
        this.page = page;
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
        final Filters filters,
        final Page requested,
        final Sorts sorts
    ) throws Exception {
        return this.origin.selection(filters, requested, sorts);
    }

    @Override
    public Iterable<Teacher> iterate() {
        return this.items;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return media.with(new PrItems("teacher", this.items))
            .with(this.page);
    }

    @Override
    public void remove(final long id) throws Exception {
        this.origin.remove(id);
    }
}
