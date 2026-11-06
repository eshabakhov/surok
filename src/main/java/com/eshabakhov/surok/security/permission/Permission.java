/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.Printable;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

/**
 * Permission to invoke a controller operation.
 * For example, a director can create a cabinet in their own school.
 *
 * @since 0.0.1
 */
public interface Permission extends Printable {

    /**
     * Evaluate the controller's authorization rule for an account and school scope.
     *
     * @param actor Account being inspected
     * @param request Current HTTP request
     * @param own Whether the requested school belongs to the account
     * @return Whether the rule grants access
     */
    boolean allows(Authentication actor, HttpServletRequest request, boolean own);
}
