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
     * Permit an operation using current global role assignments.
     *
     * @param operation Identifier declared in the operation catalog
     * @return Account after authorization
     * @throws Exception If the operation catalog cannot be read
     */
    UrAccount permit(String operation) throws Exception;

    /**
     * Permit an operation using current global and target-school role assignments.
     *
     * @param operation Identifier declared in the operation catalog
     * @param school Target school
     * @return Account after authorization
     * @throws Exception If the operation catalog cannot be read
     */
    UrAccount permit(String operation, School school) throws Exception;

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
