/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.User;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.school.building.BdsHttp;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrRole;
import java.util.Locale;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;

/**
 * Permission decoration preserves the underlying authorization rule.
 * For example, adding matrix columns cannot grant a viewer building removal access.
 *
 * @since 0.0.1
 */
final class PmRolesTest {

    @Test
    void printsEveryRoleExactlyOnce() throws Exception {
        MatcherAssert.assertThat(
            "The matrix omitted or duplicated a role column",
            new PmRoles(
                new PmMethod(
                    new HandlerMethod(
                        new BdsHttp(DSL.using(SQLDialect.POSTGRES)),
                        "remove", long.class, long.class, User.class
                    ),
                    "/schools/{school}/buildings/{building}", false
                ),
                RequestMethod.DELETE, Locale.forLanguageTag("ru")
            ).print(new MdJson()).print().path("access").findValuesAsText("role"),
            Matchers.contains(
                "ADMIN", "DIRECTOR", "DEPUTY_DIRECTOR", "TEACHER", "STUDENT",
                "BASIC_MAKER", "ADVANCED_MAKER", "PRO_MAKER", "VIEWER", "ANONYMOUS"
            )
        );
    }

    @ParameterizedTest
    @CsvSource({
        "DIRECTOR, true, true", "DIRECTOR, false, false", "DEPUTY_DIRECTOR, true, true",
        "DEPUTY_DIRECTOR, false, false", "VIEWER, true, false", "ADMIN, false, true"
    })
    void preservesTheOriginalRuleForEachSchoolScope(
        final Roles.RoleEnum role, final boolean own, final boolean expected
    ) throws Exception {
        MatcherAssert.assertThat(
            "The matrix decorator changed the original building removal rule",
            new PmRoles(
                new PmMethod(
                    new HandlerMethod(
                        new BdsHttp(DSL.using(SQLDialect.POSTGRES)),
                        "remove", long.class, long.class, User.class
                    ),
                    "/schools/{school}/buildings/{building}", false
                ),
                RequestMethod.DELETE, Locale.forLanguageTag("ru")
            ).allows(new UrRole(role), own),
            Matchers.is(expected)
        );
    }
}
