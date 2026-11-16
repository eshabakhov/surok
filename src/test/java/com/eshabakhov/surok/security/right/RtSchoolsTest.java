/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.right;

import com.eshabakhov.surok.school.SlsFake;
import java.util.List;
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
}
