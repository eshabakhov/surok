/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.media;

import com.eshabakhov.surok.federal.subject.SbFake;
import com.eshabakhov.surok.school.schedule.load.ClFake;
import com.eshabakhov.surok.school.schedule.load.hours.WhFixed;
import com.eshabakhov.surok.school.schoolclass.ScFake;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * JSON collection snapshots preserve field selection and the original medium.
 * For example, adding two classes leaves the source without an items field.
 *
 * @since 0.0.1
 */
final class MdJsonTest {

    @Test
    void keepsNestedObjectsInsideNamedValuesAndCollectionItems() throws Exception {
        final ClFake load = new ClFake(41L, new ScFake(17L), new SbFake(29L), new WhFixed(7));
        MatcherAssert.assertThat(
            "Nested load objects were flattened or converted to arrays",
            List.of(
                new MdJson().with("load", load).print().toString(),
                new MdJson().items("load", List.of(load)).print().toString()
            ),
            Matchers.contains(
                "{\"load\":{\"id\":41,\"schoolClass\":{\"id\":17},\"subject\":{\"id\":29},\"hours\":7}}",
                "{\"items\":[{\"id\":41,\"schoolClass\":{\"id\":17},\"subject\":{\"id\":29},\"hours\":7}]}"
            )
        );
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        textBlock = """
            0|items|{"title":"Классы","items":[]}
            1|items|{"title":"Классы","items":[{"id":17}]}
            2|items|{"title":"Классы","items":[{"id":17},{"id":29}]}
            2|hidden|{"title":"Классы"}
            """
    )
    void printsSelectedCollectionSnapshotsWithoutChangingTheSource(
        final int count, final String field, final String expected
    ) throws Exception {
        final MdJson source = new MdJson().with("title", "Классы")
            .include("title", field);
        MatcherAssert.assertThat(
            "Printing a collection changed the source, its selected fields or its array shape",
            List.of(
                source.items(
                    "class", List.of(new ScFake(17L), new ScFake(29L)).subList(0, count)
                ).print().toString(),
                source.print().toString()
            ),
            Matchers.contains(expected, "{\"title\":\"Классы\"}")
        );
    }
}
