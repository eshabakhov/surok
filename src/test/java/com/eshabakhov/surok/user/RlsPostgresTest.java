/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Account role assignments exclude school roles and anonymous visitors.
 * For example, ANONYMOUS cannot be stored as a user's global role.
 *
 * @since 0.0.1
 */
final class RlsPostgresTest {

    @ParameterizedTest
    @EnumSource(
        value = Roles.RoleEnum.class,
        names = {"ADMIN", "BASIC_MAKER", "ADVANCED_MAKER", "PRO_MAKER"},
        mode = EnumSource.Mode.EXCLUDE
    )
    void rejectsRolesThatCannotBeAssignedToAnAccount(final Roles.RoleEnum role) {
        Assertions.assertThrows(
            RoleGrantException.class,
            () -> new Roles.RlsPostgres(DSL.using(SQLDialect.POSTGRES), 17L).grant(role),
            "A non-account role reached global role persistence"
        );
    }
}
