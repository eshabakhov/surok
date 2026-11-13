/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

/**
 * Account details and the authentication marker of the represented user.
 * For example, an anonymous visitor has an unauthenticated, unavailable account.
 * Credential verification remains the responsibility of the authentication adapter.
 *
 * @since 0.0.1
 */
public interface Account {

    /**
     * Whether the account represents an identified application user.
     *
     * @return Whether the user is authenticated
     */
    boolean authenticated();

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
