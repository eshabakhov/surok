/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.Printable;

/**
 * Access granted to an account for a documented operation.
 * For example, a director receives school access while an administrator receives all access.
 *
 * @since 0.0.1
 */
public interface Grant extends Printable {

    /**
     * Scope of the account's access to the operation.
     *
     * @return Granted access scope
     */
    Scope scope();

    /**
     * Extent of access to an operation.
     *
     * @since 0.0.1
     */
    enum Scope {

        /** Access regardless of school membership. */
        ALL,

        /** Access limited to the account's own schools. */
        SCHOOL,

        /** No access to the operation. */
        DENIED
    }
}
