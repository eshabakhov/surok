/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok;

import java.time.Instant;

/**
 * Represents an error response with message and timestamp.
 *
 * @since 0.0.1
 */
public interface ErrorResponse extends Printable {

    @Override
    default <T> Media<T> print(final Media<T> media) {
        return media.with("message", this.message())
            .attribute("timestamp", this.timestamp().toString());
    }

    /**
     * Human-readable error message.
     *
     * @return Message
     */
    String message();

    /**
     * Timestamp of the error occurrence.
     *
     * @return Timestamp
     */
    Instant timestamp();
}
