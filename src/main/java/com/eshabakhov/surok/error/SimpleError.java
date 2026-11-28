/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.error;

import com.eshabakhov.surok.ErrorResponse;
import com.eshabakhov.surok.Media;

/**
 * Public error message. For example, a JSON response has a single message property.
 *
 * @since 0.0.1
 */
public final class SimpleError implements ErrorResponse {

    /** Human-readable error message. */
    private final String message;

    /**
     * New error.
     *
     * @param message Public message
     */
    public SimpleError(final String message) {
        this.message = message;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) {
        return media.with("message", this.message);
    }
}
