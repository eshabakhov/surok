/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.security.right.Right;
import com.eshabakhov.surok.security.right.RtSchoolMember;
import com.eshabakhov.surok.user.Roles;
import java.util.Set;

/**
 * Permission granted by a school role listed in the specification.
 * For example, a director may create a building in their own school.
 *
 * @since 0.0.1
 */
final class PmSchoolRoles implements Permission {

    /** Roles accepted by this grant. */
    private final Set<Roles.RoleEnum> roles;

    /**
     * New role grant.
     *
     * @param roles Declared roles
     */
    PmSchoolRoles(final Set<Roles.RoleEnum> roles) {
        this.roles = roles;
    }

    @Override
    public Right right(final UrAccount actor, final School school) {
        return () -> new RtSchoolMember(actor, school).allows()
            && this.roles.stream().anyMatch(school.users().roles(actor)::contains);
    }
}
