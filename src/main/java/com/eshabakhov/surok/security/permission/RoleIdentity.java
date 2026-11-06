/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import java.security.Principal;
import java.util.Set;

/**
 * Representative account used only to inspect a role's school scope.
 * Its public shape supports the same school membership expressions as AuthUser.
 *
 * @since 0.0.1
 */
public interface RoleIdentity extends Principal {

    /**
     * Account information exposed to authorization expressions.
     *
     * @return Representative school membership
     */
    RoleIdentity info();

    /**
     * Representative school identifier.
     *
     * @return School used to compare local and foreign access
     */
    Set<Long> schools();
}
