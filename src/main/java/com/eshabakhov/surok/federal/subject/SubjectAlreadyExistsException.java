/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

/**
 * Subject already exists.
 *
 * @since 0.0.1
 */
public final class SubjectAlreadyExistsException extends Exception {

    /**
     * New exception.
     *
     * @param message Error message
     * @since 0.0.1
     */
    public SubjectAlreadyExistsException(final String message) {
        super(message);
    }
}
