/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement;

import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInput;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInputGrade;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInputHours;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInputPart;
import com.eshabakhov.surok.federal.subject.SbsFederal;
import com.eshabakhov.surok.media.MdJson;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;

/**
 * Unsaved requirement fields without database access for empty or optional requirements.
 * For example, an optional draft preserves an explicitly entered zero-hour allowance.
 *
 * @since 0.0.1
 */
final class RqDraftTest {

    /**
     * Prints empty fields and a revised optional requirement without inserting rows.
     *
     * @throws Exception If printing fails
     */
    @Test
    void printsEmptyAndOptionalFields() throws Exception {
        final Requirement draft = new RqDraft(
            new RqInput(), new SbsFederal(DSL.using(SQLDialect.POSTGRES))
        );
        MatcherAssert.assertThat(
            "The requirement draft lost zero hours, changed its original fields or read storage",
            List.of(
                draft.revise(
                    new RqInputHours(
                        new RqInputPart(new RqInputGrade(new RqInput(), "7"), "OPTIONAL"), "0"
                    )
                ).print(new MdJson()).print(),
                draft.print(new MdJson()).print()
            ),
            Matchers.contains(
                JsonNodeFactory.instance.objectNode().put("grade", "7")
                    .put("hours", "0").put("partType", "OPTIONAL"),
                JsonNodeFactory.instance.objectNode().put("grade", "")
                    .put("hours", "").put("partType", "")
            )
        );
    }
}
