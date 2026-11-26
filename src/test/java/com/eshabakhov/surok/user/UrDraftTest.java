/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.media.MdJson;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Public registration fields of a user who has no account yet.
 * For example, submitted fields can be shown again without retaining credentials.
 *
 * @since 0.0.1
 */
final class UrDraftTest {

    /**
     * Prints empty and submitted public fields without an account identity.
     *
     * @throws Exception If printing fails
     */
    @Test
    void printsRegistrationFieldsWithoutCredentials() throws Exception {
        MatcherAssert.assertThat(
            "The registration draft lost public fields or exposed account credentials",
            List.of(
                new UrDraft("", "").print(new MdJson()).print(),
                new UrDraft("Анна", "anna@example.org").print(new MdJson()).print()
            ),
            Matchers.contains(
                JsonNodeFactory.instance.objectNode().put("username", "").put("email", ""),
                JsonNodeFactory.instance.objectNode().put("username", "Анна")
                    .put("email", "anna@example.org")
            )
        );
    }

    /** Keeps registration fields from granting any account roles. */
    @Test
    void retainsTheAnonymousRole() {
        MatcherAssert.assertThat(
            "The registration draft acquired an authenticated role",
            new UrDraft("Анна", "anna@example.org").roles().contains(Roles.RoleEnum.ANONYMOUS),
            Matchers.is(true)
        );
    }
}
