/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.error;

import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.media.MdXml;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
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
        final SimpleError error = new SimpleError(
            "Недопустимая роль", Instant.parse("2026-11-16T07:23:41.123Z")
        );
        MatcherAssert.assertThat(
            "The explicit error representation lost its message or timestamp",
            List.of(error.print(new MdJson()).print(), error.print(new MdXml("error")).print()),
            Matchers.contains(
                new ObjectMapper().readTree(
                    """
                    {"message":"Недопустимая роль","timestamp":"2026-11-16T07:23:41.123Z"}
                    """
                ),
                String.join(
                    "", "<error timestamp=\"2026-11-16T07:23:41.123Z\">",
                    "<message>Недопустимая роль</message></error>"
                )
            )
        );
    }
}
