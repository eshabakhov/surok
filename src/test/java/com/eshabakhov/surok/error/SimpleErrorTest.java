/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.error;

import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.media.MdXml;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Explicit error serialization without Jackson field discovery.
 * For example, an error containing XML markup can be read as plain text.
 *
 * @since 0.0.1
 */
final class SimpleErrorTest {

    @Test
    void printsOnlyThePublicMessage() throws Exception {
        final SimpleError error = new SimpleError("Недопустимая роль <учитель>");
        MatcherAssert.assertThat(
            "The error message changed or gained extra fields",
            List.of(error.print(new MdJson()).print(), error.print(new MdXml("error")).print()),
            Matchers.contains(
                new ObjectMapper().readTree("{\"message\":\"Недопустимая роль <учитель>\"}"),
                "<error><message>Недопустимая роль &lt;учитель&gt;</message></error>"
            )
        );
    }
}
