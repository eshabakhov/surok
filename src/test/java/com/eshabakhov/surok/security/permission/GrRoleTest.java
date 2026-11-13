/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.User;
import com.eshabakhov.surok.school.building.BdsHttp;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrAnonymous;
import com.eshabakhov.surok.user.UrRole;
import java.util.Locale;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.method.HandlerMethod;

/**
 * Access scopes evaluated independently from their representations.
 * For example, a director may remove a building only within their own school.
 *
 * @since 0.0.1
 */
final class GrRoleTest {

    @ParameterizedTest
    @CsvSource({
        "ADMIN, ALL", "DIRECTOR, SCHOOL", "DEPUTY_DIRECTOR, SCHOOL",
        "TEACHER, DENIED", "STUDENT, DENIED", "VIEWER, DENIED",
        "BASIC_MAKER, DENIED", "ADVANCED_MAKER, DENIED", "PRO_MAKER, DENIED"
    })
    void evaluatesTheSchoolScopeForEachRole(
        final Roles.RoleEnum role, final Grant.Scope expected
    ) throws Exception {
        MatcherAssert.assertThat(
            "The role received an incorrect building removal scope",
            new GrRole(
                new PmMethod(
                    new HandlerMethod(
                        new BdsHttp(DSL.using(SQLDialect.POSTGRES)),
                        "remove", long.class, long.class, User.class
                    ),
                    "/schools/{school}/buildings/{building}", false
                ),
                new UrRole(role), role, Locale.forLanguageTag("ru")
            ).scope(),
            Matchers.is(expected)
        );
    }

    @Test
    void deniesAnAnonymousVisitorWithoutAccessingAccountData() throws Exception {
        MatcherAssert.assertThat(
            "An anonymous visitor received building removal access",
            new GrRole(
                new PmMethod(
                    new HandlerMethod(
                        new BdsHttp(DSL.using(SQLDialect.POSTGRES)),
                        "remove", long.class, long.class, User.class
                    ),
                    "/schools/{school}/buildings/{building}", false
                ),
                new UrAnonymous(), Roles.RoleEnum.ANONYMOUS, Locale.forLanguageTag("ru")
            ).scope(),
            Matchers.is(Grant.Scope.DENIED)
        );
    }
}
