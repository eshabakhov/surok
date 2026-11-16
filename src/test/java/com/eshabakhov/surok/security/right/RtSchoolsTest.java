/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.right;

import com.eshabakhov.surok.UrAccount;
import com.eshabakhov.surok.school.SlsFake;
import com.eshabakhov.surok.user.Roles;
import com.eshabakhov.surok.user.UrAcRole;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Rights evaluated against the explicitly supplied schools.
 * For example, being a director elsewhere does not grant access to an unrelated school.
 *
 * @since 0.0.1
 */
final class RtSchoolsTest {

    @Test
    void deniesAccessWhenNoSchoolsAreSupplied() {
        MatcherAssert.assertThat(
            "An empty school collection granted access",
            new RtSchools(new SlsFake(List.of()), school -> () -> true).allows(),
            Matchers.is(false)
        );
    }

    @Test
    void deniesAccessToAnotherAccountsSchools() {
        final UrAccount actor = new UrAcRole(
            ThreadLocalRandom.current().nextLong(1L, Long.MAX_VALUE - 1L), Roles.RoleEnum.DIRECTOR
        );
        MatcherAssert.assertThat(
            "A director received access to another account's schools",
            new RtSchools(
                new UrAcRole(actor.uid() + 1L, Roles.RoleEnum.DIRECTOR).schools(),
                school -> new RtSchoolDirector(actor, school)
            ).allows(),
            Matchers.is(false)
        );
    }

    @Test
    void allowsAccessWhenALaterSchoolGrantsTheRight() {
        final UrAccount actor = new UrAcRole(
            ThreadLocalRandom.current().nextLong(1L, Long.MAX_VALUE - 1L), Roles.RoleEnum.DIRECTOR
        );
        MatcherAssert.assertThat(
            "A permitted school was ignored after an unrelated school",
            new RtSchools(
                new SlsFake(
                    List.of(
                        new UrAcRole(actor.uid() + 1L, Roles.RoleEnum.DIRECTOR)
                            .schools().iterate().iterator().next(),
                        actor.schools().iterate().iterator().next()
                    )
                ),
                school -> new RtSchoolDirector(actor, school)
            ).allows(),
            Matchers.is(true)
        );
    }
}
