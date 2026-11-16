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
 * Anonymous access with a dedicated role and without account data.
 * For example, an anonymous visitor cannot acquire an administrator role.
 *
 * @since 0.0.1
 */
final class UrAnonymousTest {

    @Test
    void deniesAdministratorAccess() {
        MatcherAssert.assertThat(
            "An anonymous visitor received administrator access",
            new RtAdmin(new UrAnonymous()).allows(), Matchers.is(false)
        );
    }

    @Test
    void hasTheAnonymousRole() {
        MatcherAssert.assertThat(
            "An anonymous visitor did not have the anonymous role",
            new UrAnonymous().roles().contains(Roles.RoleEnum.ANONYMOUS), Matchers.is(true)
        );
    }

    @Test
    void hasOnlyTheAnonymousRole() {
        MatcherAssert.assertThat(
            "An anonymous visitor had unexpected roles",
            new UrAnonymous().roles().list().stream().map(Role::name).toList(),
            Matchers.contains(Roles.RoleEnum.ANONYMOUS.name())
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
}
