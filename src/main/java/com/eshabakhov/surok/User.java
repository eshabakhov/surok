/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok;

import com.eshabakhov.surok.user.Account;
import com.eshabakhov.surok.user.Roles;

/**
 * User abstraction.
 *
 * @since 0.0.1
 */
public interface User extends Printable {

    /**
     * User unique identifier.
     *
     * @return User ID
     */
    long uid();

    /**
     * Account details.
     *
     * @return Registered account
     */
    Account account();

    /**
     * User roles.
     *
     * @return Roles
     */
    Roles roles();

    /**
     * Schools available to this user.
     *
     * @return User's schools
     */
    Schools schools();
}
