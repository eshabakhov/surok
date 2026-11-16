/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.error;

import com.eshabakhov.surok.media.MdJson;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Explicit error serialization without Jackson field discovery.
 * For example, a timestamp retains its ISO-8601 representation and fractional seconds.
 *
 * @since 0.0.1
 */
final class SimpleErrorTest {

    @Test
    void printsTheErrorWithAnIsoTimestamp() throws Exception {
        MatcherAssert.assertThat(
            "The explicit error representation lost its message or timestamp",
            new SimpleError("Недопустимая роль", Instant.parse("2026-11-16T07:23:41.123Z"))
                .print(new MdJson()).print(),
            Matchers.is(
                new ObjectMapper().readTree(
                    """
                    {"message":"Недопустимая роль","timestamp":"2026-11-16T07:23:41.123Z"}
                    """
                )
            )
        );
    }
}
