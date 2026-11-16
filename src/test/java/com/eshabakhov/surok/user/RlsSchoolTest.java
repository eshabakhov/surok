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
 * School role assignments exclude global roles and anonymous visitors.
 * For example, ANONYMOUS cannot be granted to a school member.
 *
 * @since 0.0.1
 */
final class RlsSchoolTest {

    @ParameterizedTest
    @EnumSource(
        value = Roles.RoleEnum.class,
        names = {"DIRECTOR", "DEPUTY_DIRECTOR", "TEACHER", "STUDENT", "VIEWER"},
        mode = EnumSource.Mode.EXCLUDE
    )
    void rejectsRolesThatCannotBeAssignedWithinASchool(final Roles.RoleEnum role) {
        Assertions.assertThrows(
            RoleGrantException.class,
            () -> new RlsSchool(DSL.using(SQLDialect.POSTGRES), 29L, 43L).grant(role),
            "A non-school role reached school membership persistence"
        );
    }
}
