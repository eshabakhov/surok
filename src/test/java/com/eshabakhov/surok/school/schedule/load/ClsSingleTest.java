/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schedule.load;

import com.eshabakhov.surok.federal.subject.SbFake;
import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.media.MdXml;
import com.eshabakhov.surok.school.schedule.load.hours.WhFake;
import com.eshabakhov.surok.school.schoolclass.ScFake;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

/**
 * A single selected load retains the collection contract in both formats.
 * For example, its nested class and subject keep their own identities.
 *
 * @since 0.0.1
 */
final class ClsSingleTest {

    /**
     * Preserves identities, nested resources, and the collection wrapper.
     *
     * @throws Exception If the selected load cannot be printed
     */
    @Test
    void printsACollectionWithNestedResources() throws Exception {
        final ClassLoads loads = new ClsSingle(
            new ClassLoadsPostgres(DSL.using(SQLDialect.POSTGRES), 13L),
            new ClFake(19L, new ScFake(23L), new SbFake(37L), new WhFake(3))
        );
        MatcherAssert.assertThat(
            "The selected load lost its collection or nested resource identities",
            List.of(
                loads.print(new MdJson()).print().toString(),
                loads.print(new MdXml("loads")).print()
            ),
            Matchers.contains(
                """
                    {"items":[{"id":19,"schoolClass":{"id":23},"subject":{"id":37},"hours":3}]}
                    """.strip(),
                String.join(
                    "", "<loads><load id=\"19\"><schoolClass id=\"23\"></schoolClass>",
                    "<subject id=\"37\"></subject><hours>3</hours></load></loads>"
                )
            )
        );
    }
}
