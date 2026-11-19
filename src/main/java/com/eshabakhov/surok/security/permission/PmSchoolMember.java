/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.security.right.Right;
import com.eshabakhov.surok.security.right.RtSchoolMember;

/**
 * Permission granted by membership alone.
 * For example, any member may read their school's buildings.
 *
 * @since 0.0.1
 */
final class PmSchoolMember implements Permission {

    @Override
    public Right right(final UrAccount actor, final School school) {
        return new RtSchoolMember(actor, school);
    }
}
