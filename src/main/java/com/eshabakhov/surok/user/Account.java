/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.Printable;

/**
 * Details of a registered account, independent of the current login session.
 * For example, a deleted account cannot be used for password login.
 * Credential verification remains the responsibility of the authentication adapter.
 *
 * @since 0.0.1
 */
public interface Account extends Printable {

    /**
     * Login name of the account.
     *
     * @return Username
     */
    String username();

    /**
     * Stored password hash used by the authentication adapter.
     *
     * @return Encoded password
     */
    String password();

    /**
     * Whether the account belongs to a corporate user.
     *
     * @return Corporate account marker
     */
    boolean corporate();

    /**
     * Whether the account remains available for use.
     *
     * @return Account availability marker
     */
    boolean alive();
}
