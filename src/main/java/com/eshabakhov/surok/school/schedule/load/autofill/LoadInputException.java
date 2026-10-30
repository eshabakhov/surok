/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load.autofill;

/**
 * Invalid teaching load input.
 *
 * @since 0.0.1
 */
public final class LoadInputException extends Exception {

    /** Serialization identifier. */
    private static final long serialVersionUID = 1L;

    /**
     * New input exception.
     *
     * @param message Error message
     */
    public LoadInputException(final String message) {
        super(message);
    }
}
