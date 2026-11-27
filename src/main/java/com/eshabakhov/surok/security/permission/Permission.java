/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import org.springframework.security.access.AccessDeniedException;

/**
 * Permission already bound to its caller and target.
 * For example, permit a collection before invoking its creation operation.
 *
 * @since 0.0.1
 */
@FunctionalInterface
public interface Permission {

    /**
     * Evaluate current assignments.
     *
     * @return Whether access is granted
     */
    boolean allows();

    /**
     * Expose an object after evaluating access.
     *
     * @param origin Protected object
     * @param <T> Object type
     * @return Permitted object
     */
    default <T> T permit(final T origin) {
        if (!this.allows()) {
            throw new AccessDeniedException("Operation access denied");
        }
        return origin;
    }
}
