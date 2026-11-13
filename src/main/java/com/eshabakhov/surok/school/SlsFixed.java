/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.PrItems;
import com.eshabakhov.surok.School;
import com.eshabakhov.surok.Schools;
import com.eshabakhov.surok.Sorts;
import java.util.Collection;
import java.util.List;

/**
 * Immutable schools available for identity and membership inspection.
 * For example, {@code new SlsFixed(List.of())} represents an anonymous visitor's schools.
 *
 * @since 0.0.1
 */
public final class SlsFixed implements Schools {

    /** Represented schools. */
    private final List<School> items;

    /**
     * New school snapshot.
     *
     * @param items Available schools
     */
    public SlsFixed(final Collection<School> items) {
        this(List.copyOf(items));
    }

    /**
     * New school snapshot backed by an immutable list.
     *
     * @param items Immutable schools
     */
    private SlsFixed(final List<School> items) {
        this.items = items;
    }

    @Override
    public boolean contains(final long id) {
        return this.items.stream().anyMatch(school -> school.uid() == id);
    }

    @Override
    public School create(final SchoolInput input) {
        throw new UnsupportedOperationException("Read-only schools cannot create a school");
    }

    @Override
    public School school(final long id) throws SchoolNotFoundException {
        return this.items.stream().filter(school -> school.uid() == id).findFirst().orElseThrow(
            () -> new SchoolNotFoundException(String.format("School with id=%d not found", id))
        );
    }

    @Override
    public Schools selection(final Filters filters, final Page page, final Sorts sorts) {
        throw new UnsupportedOperationException("School snapshots cannot execute selections");
    }

    @Override
    public Iterable<School> iterate() {
        return this.items;
    }

    @Override
    public void remove(final long id) {
        throw new UnsupportedOperationException(
            String.format("Read-only schools cannot remove school %d", id)
        );
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return media.with(new PrItems("school", this.items));
    }
}
