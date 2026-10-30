/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load;

import com.eshabakhov.surok.federal.subject.SbFake;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.school.schedule.load.hours.WhFixed;
import com.eshabakhov.surok.school.schoolclass.ScFake;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Versioned loads expose their public representation.
 * For example, class and subject identities are included with weekly hours.
 *
 * @since 0.0.1
 */
final class ClV1Test {

    @Test
    void printsTheVersionedLoad() throws Exception {
        MatcherAssert.assertThat(
            "The versioned load changed its class, subject, or current hours",
            new ClV1(
                new ClFake(41L, new ScFake(17L), new SbFake(29L), new WhFixed(7))
            ).print(new MdJson()).print(),
            Matchers.equalTo(
                JsonNodeFactory.instance.objectNode().put("id", 41L).put("hours", 7).setAll(
                    Map.of(
                        "schoolClass", JsonNodeFactory.instance.objectNode().put("id", 17L),
                        "subject", JsonNodeFactory.instance.objectNode().put("id", 29L)
                    )
                )
            )
        );
    }
}
