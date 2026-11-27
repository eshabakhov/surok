/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.user;

import com.eshabakhov.surok.media.MdJson;
import com.eshabakhov.surok.media.MdXml;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Role collections use domain elements and preserve empty JSON arrays.
 * For example, a visitor's roles print without an additional list wrapper.
 *
 * @since 0.0.1
 */
final class RolesTest {

    /**
     * Preserves the collection shape independently of the role count.
     *
     * @param count Number of roles
     * @param json Expected JSON array
     * @param xml Expected domain elements
     * @throws Exception If roles cannot be printed
     */
    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        textBlock = """
            0|[]|''
            1|[{"name":"ANONYMOUS"}]|<role><name>ANONYMOUS</name></role>
            """
    )
    void printsCollectionsWithoutAnExtraWrapper(
        final int count, final String json, final String xml
    ) throws Exception {
        final Roles roles = new RlsFake(
            List.<Role>of(new RlNamed(Roles.RoleEnum.ANONYMOUS)).subList(0, count)
        );
        MatcherAssert.assertThat(
            "The roles gained an extra wrapper or lost their empty array",
            List.of(
                roles.print(new MdJson()).print().toString(),
                roles.print(new MdXml("roles")).print()
            ),
            Matchers.contains(
                String.format("{\"items\":%s}", json),
                String.format("<roles>%s</roles>", xml)
            )
        );
    }
}
