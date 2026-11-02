/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement;

/**
 * Federal curriculum requirement already exists.
 *
 * @since 0.0.1
 */
public final class RequirementAlreadyExistsException extends Exception {

    /**
     * New exception.
     *
     * @since 0.0.1
     */
    public RequirementAlreadyExistsException() {
        this(null);
    }

    /**
     * New conflict preserving the database constraint failure.
     *
     * @param cause Original failure
     */
    public RequirementAlreadyExistsException(final Throwable cause) {
        super("Federal curriculum requirement already exists", cause);
    }
}
