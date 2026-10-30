/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

import com.eshabakhov.surok.Filters;
import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.Sorts;
import java.util.List;

/**
 * Subjects representation containing one selected subject.
 *
 * @since 0.0.1
 */
public final class SbsSingle implements Subjects {

    /** Origin subjects. */
    private final Subjects origin;

    /** Selected subject. */
    private final Subject selected;

    /**
     * New single-subject representation.
     *
     * @param origin Origin subjects
     * @param selected Selected subject
     */
    public SbsSingle(final Subjects origin, final Subject selected) {
        this.origin = origin;
        this.selected = selected;
    }

    @Override
    public Subject create(final SubjectInput input) throws Exception {
        return this.origin.create(input);
    }

    @Override
    public Subject subject(final long id) throws Exception {
        return this.origin.subject(id);
    }

    @Override
    public Subjects selection(
        final Filters filters,
        final Page page,
        final Sorts sorts
    ) throws Exception {
        return this.origin.selection(filters, page, sorts);
    }

    @Override
    public Iterable<Subject> iterate() {
        return List.of(this.selected);
    }

    @Override
    public void remove(final long id) throws Exception {
        this.origin.remove(id);
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return media.with("subject", this.selected);
    }
}
