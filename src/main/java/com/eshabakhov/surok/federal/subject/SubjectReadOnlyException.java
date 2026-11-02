/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.subject;

/**
 * A school attempted to modify a shared federal subject.
 * For example, removing a federal entry through school subjects raises this failure.
 *
 * @since 0.0.1
 */
public final class SubjectReadOnlyException extends Exception {

    /**
     * New rejected school operation.
     *
     * @param subject Shared subject identifier
     */
    public SubjectReadOnlyException(final long subject) {
        super(String.format("Federal subject %d cannot be changed by a school", subject));
    }
}
