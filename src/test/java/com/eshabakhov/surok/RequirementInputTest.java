/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok;

import com.eshabakhov.surok.federal.curriculum.requirement.input.RequirementInputException;
import com.eshabakhov.surok.federal.curriculum.requirement.input.RqInputHttp;
import com.eshabakhov.surok.media.MdJson;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Tests federal curriculum requirement input.
 *
 * @since 0.0.1
 */
final class RequirementInputTest {

    @Test
    void printsDecoratedHttpInput() throws Exception {
        final MultiValueMap<String, String> parameters =
            new LinkedMultiValueMap<>();
        parameters.add("grade", " 5 ");
        parameters.add("subjectId", " 173 ");
        parameters.add("hours", " 4 ");
        parameters.add("partType", "MANDATORY");
        Assertions.assertEquals(
            JsonNodeFactory.instance.objectNode()
                .put("grade", 5)
                .put("subjectId", "173")
                .put("hours", 4)
                .put("partType", "MANDATORY"),
            new RqInputHttp(parameters).print(new MdJson()).print()
        );
    }

    @Test
    void rejectsMissingRequiredInput() {
        Assertions.assertThrows(
            RequirementInputException.class,
            () -> new RqInputHttp(
                new LinkedMultiValueMap<>(0)
            ).print(new MdJson())
        );
    }
}
