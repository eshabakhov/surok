/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Anonymous accounts expose their absence without inventing credentials.
 * For example, an anonymous account cannot be enabled for password authentication.
 *
 * @since 0.0.1
 */
final class AcAnonymousTest {

    @Test
    void deniesAuthenticationAndAccountAvailability() {
        final Account account = new AcAnonymous("посетитель");
        MatcherAssert.assertThat(
            "An anonymous account claimed authentication or account availability",
            Map.of(
                "authenticated", account.authenticated(),
                "alive", account.alive(), "corporate", account.corporate()
            ),
            Matchers.is(Map.of("authenticated", false, "alive", false, "corporate", false))
        );
    }

    @Test
    void refusesToInventAUsername() {
        Assertions.assertThrows(
            IllegalStateException.class, () -> new AcAnonymous("гость").username(),
            "An anonymous account invented a username"
        );
    }

    @Test
    void refusesToInventAPassword() {
        Assertions.assertThrows(
            IllegalStateException.class, () -> new AcAnonymous("посетитель").password(),
            "An anonymous account invented a password"
        );
    }
}
