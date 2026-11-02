/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.autofill;

/**
 * A planning draft is unavailable for the requested schedule.
 * For example, reading a schedule before its first draft was saved raises this exception.
 *
 * @since 0.0.1
 */
public final class LoadDraftNotFoundException extends Exception {

    /** Serialization identifier. */
    private static final long serialVersionUID = 1L;

    /**
     * New unavailable draft error.
     *
     * @param schedule Schedule identifier
     */
    public LoadDraftNotFoundException(final long schedule) {
        super(String.format("No available load draft for schedule %d", schedule));
    }
}
