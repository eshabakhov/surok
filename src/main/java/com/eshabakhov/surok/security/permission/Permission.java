/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.School;
import com.eshabakhov.surok.Schools;
import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.security.right.Right;
import com.eshabakhov.surok.security.right.RtSchools;

/**
 * Authorization shared by domain objects and HTTP operations.
 * For example, evaluate a creation grant against the school supplied by a request.
 *
 * @since 0.0.1
 */
@FunctionalInterface
public interface Permission {

    /**
     * Bind the rule to a visitor without an account.
     *
     * @return Anonymous access, denied unless explicitly granted
     */
    default Right right() {
        return () -> false;
    }

    /**
     * Bind the rule to the target school.
     *
     * @param actor Current account
     * @param school Target school
     * @return Right evaluated against current assignments
     */
    Right right(UrAccount actor, School school);

    /**
     * Bind the rule to the schools considered by the permission catalog.
     *
     * @param actor Current account
     * @param schools Schools to inspect
     * @return Right to see this operation
     */
    default Right right(final UrAccount actor, final Schools schools) {
        return new RtSchools(schools, school -> this.right(actor, school));
    }
}
