/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.security.right.RtAdmin;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Anonymous access without account data or school memberships.
 * For example, an anonymous visitor cannot acquire an administrator role.
 *
 * @since 0.0.1
 */
final class UrAnonymousTest {

    @Test
    void providesAnAccountThatCannotBeEnabledForLogin() {
        MatcherAssert.assertThat(
            "An anonymous visitor provided an account enabled for login",
            new AuthUser(new UrAnonymous()).isEnabled(), Matchers.is(false)
        );
    }

    @Test
    void deniesAdministratorAccess() {
        MatcherAssert.assertThat(
            "An anonymous visitor received administrator access",
            new RtAdmin(new UrAnonymous()).allows(), Matchers.is(false)
        );
    }

    @Test
    void exposesNoAssignedRoles() {
        MatcherAssert.assertThat(
            "An anonymous visitor had assigned roles",
            new UrAnonymous().roles().list(), Matchers.empty()
        );
    }

    @Test
    void belongsToNoSchools() {
        MatcherAssert.assertThat(
            "An anonymous visitor belonged to a school",
            new UrAnonymous().schools().iterate(), Matchers.emptyIterable()
        );
    }

    @Test
    void cannotAcquireAnAdministratorRole() {
        Assertions.assertThrows(
            UnsupportedOperationException.class,
            () -> new UrAnonymous().roles().grant(Roles.RoleEnum.ADMIN),
            "An anonymous visitor acquired an administrator role"
        );
    }

    @Test
    void refusesToImpersonateAnAccount() {
        Assertions.assertThrows(
            IllegalStateException.class, () -> new UrAnonymous().uid(),
            "An anonymous visitor exposed an account identifier"
        );
    }
}
