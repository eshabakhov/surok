/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.right;

/**
 * Right granted when either of two independent rights permits an operation.
 * For example, an administrator or a school director may revise a school.
 *
 * @since 0.0.1
 */
public final class RtEither implements Right {

    /** First right to check. */
    private final Right first;

    /** Second right to check. */
    private final Right second;

    /**
     * New alternative right.
     *
     * @param first First right
     * @param second Second right
     */
    public RtEither(final Right first, final Right second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public boolean allows() {
        return this.first.allows() || this.second.allows();
    }
}
