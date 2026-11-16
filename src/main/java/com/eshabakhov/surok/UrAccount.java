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

    @Override
    long uid();

    @Override
    Account account();

    @Override
    Schools schools();
}
