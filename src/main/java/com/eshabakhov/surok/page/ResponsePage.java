/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.page;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Page;
import com.eshabakhov.surok.ResultPage;
import lombok.EqualsAndHashCode;

/**
 * Pagination state for a completed query.
 *
 * @since 0.0.1
 */
@EqualsAndHashCode
public final class ResponsePage implements ResultPage {

    /** Request pagination. */
    private final Page origin;

    /** Total number of matching items. */
    private final int amount;

    /**
     * New response page.
     *
     * @param origin Request pagination
     * @param amount Total number of matching items
     */
    public ResponsePage(final Page origin, final int amount) {
        this.origin = origin;
        this.amount = amount;
    }

    @Override
    public int limit() {
        return this.origin.limit();
    }

    @Override
    public int offset() {
        return this.origin.offset();
    }

    @Override
    public int total() {
        return this.amount;
    }

    @Override
    public int totalPages() {
        return (int) Math.ceil((double) this.amount / this.limit());
    }

    @Override
    public boolean hasNext() {
        return this.amount > (long) this.offset() * this.limit();
    }

    @Override
    public boolean hasPrev() {
        return this.offset() > 1;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) {
        return media.attribute("limit", this.limit())
            .attribute("offset", this.offset())
            .attribute("total", this.total())
            .attribute("totalPages", this.totalPages())
            .attribute("hasNext", this.hasNext())
            .attribute("hasPrev", this.hasPrev());
    }
}
