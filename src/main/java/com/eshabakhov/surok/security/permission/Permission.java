/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.Printable;
import com.eshabakhov.surok.Schools;
import com.eshabakhov.surok.UrAccount;

/**
 * Permission to invoke a controller operation.
 * For example, a director can create a cabinet in their own school.
 *
 * @since 0.0.1
 */
public interface Permission extends Printable {

    /**
     * Evaluate access for a visitor without an account.
     *
     * @return Whether the operation is public
     */
    boolean allows();

    /**
     * Evaluate the controller's authorization rule for an account in the supplied schools.
     *
     * @param actor Account being inspected
     * @param schools Schools in which access is evaluated
     * @return Whether the rule grants access
     */
    boolean allows(UrAccount actor, Schools schools);
}
