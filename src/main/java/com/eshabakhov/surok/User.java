/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok;

import com.eshabakhov.surok.user.Roles;

/**
 * Application visitor, with or without a registered account.
 * For example, an anonymous visitor has the ANONYMOUS role.
 *
 * @since 0.0.1
 */
public interface User extends Printable {

    /**
     * User roles.
     *
     * @return Roles
     */
    Roles roles();
}
