/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok;

import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.media.MdXml;
import com.eshabakhov.surok.school.schoolclass.ScFake;
import java.util.List;
import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Composed representations retain original fields, numbers and nested collections.
 * For example, a class can be printed alongside its hours without a domain wrapper.
 *
 * @since 0.0.1
 */
final class PrFieldsTest {

    @Test
    void printsOriginalFieldsWithNumbersAndNestedCollections() throws Exception {
        final Printable fields = new PrFields(
            List.of(new ScFake(17L)), Map.of("hours", 3),
            Map.of("subjects", new PrItems("subject", List.of()))
        );
        MatcherAssert.assertThat(
            "The composed representation lost original fields, hours or an empty collection",
            List.of(
                fields.print(new MdJson()).print().toString(),
                fields.print(new MdXml("class")).print()
            ),
            Matchers.contains(
                "{\"id\":17,\"hours\":3,\"subjects\":[]}",
                "<class><id>17</id><hours>3</hours><subjects></subjects></class>"
            )
        );
    }
}
