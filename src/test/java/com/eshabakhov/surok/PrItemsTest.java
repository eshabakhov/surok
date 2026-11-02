/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok;

import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.media.MdXml;
import com.eshabakhov.surok.school.schoolclass.ScFake;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Named collections rendered consistently when empty or populated.
 * For example, an empty class selection remains an array in JSON.
 *
 * @since 0.0.1
 */
final class PrItemsTest {

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        textBlock = """
            0|[]|''
            1|[{"id":17}]|<class><id>17</id></class>
            2|[{"id":17},{"id":29}]|<class><id>17</id></class><class><id>29</id></class>
            """
    )
    void printsNamedCollectionsInBothFormats(
        final int count, final String json, final String xml
    ) throws Exception {
        final PrItems items = new PrItems(
            "class", List.of(new ScFake(17L), new ScFake(29L)).subList(0, count)
        );
        MatcherAssert.assertThat(
            "The named collection lost its items or changed its empty representation",
            List.of(
                new MdJson().with("classes", items).print().toString(),
                new MdXml("draft").with("classes", items).print()
            ),
            Matchers.contains(
                String.format("{\"classes\":%s}", json),
                String.format("<draft><classes>%s</classes></draft>", xml)
            )
        );
    }
}
