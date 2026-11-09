/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.right;

import org.springframework.security.access.AccessDeniedException;

/**
 * Permission to use an object for a domain operation.
 * For example, a school right permits buildings before their creation method is called.
 *
 * @since 0.0.1
 */
@FunctionalInterface
public interface Right {

    /**
     * Whether the current actor may use the guarded object.
     *
     * @return True when access is granted
     */
    boolean allows();

    /**
     * Return an object only if the current actor may use it.
     *
     * @param origin Object to guard
     * @param <T> Object type
     * @return Permitted object
     */
    default <T> T permit(final T origin) {
        if (!this.allows()) {
            throw new AccessDeniedException("School resource access denied");
        }
        return origin;
    }
}
