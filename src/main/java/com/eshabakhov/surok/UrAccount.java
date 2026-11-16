/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok;

import com.eshabakhov.surok.user.Account;

/**
 * User with a registered account, independently of the current login session.
 * For example, school membership can reference this user without accepting anonymous visitors.
 *
 * @since 0.0.1
 */
public interface UrAccount extends User {

    /**
     * Registered user identifier.
     *
     * @return User identifier
     */
    long uid();

    /**
     * Account details.
     *
     * @return Registered account
     */
    Account account();

    /**
     * Schools available to this user.
     *
     * @return User's schools
     */
    Schools schools();
}
