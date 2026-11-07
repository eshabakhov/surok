/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Yaml;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Tests component traversal in an already filtered description.
 * For example, a recursive schema can remain visible without exposing unused models.
 *
 * @since 0.0.1
 */
final class OaComponentsTest {

    /**
     * Keeps an entire schema referenced through one of its properties.
     *
     * @throws Exception If the fixture cannot be read
     */
    @Test
    void followsNestedAndRecursiveComponentReferences() throws Exception {
        final ObjectNode document = Yaml.mapper().readValue(
            """
            paths:
                /дерево:
                    get:
                        responses:
                            "200":
                                $ref: "#/components/responses/Branch"
            components:
                responses:
                    Branch:
                        description: Ветка
                        content:
                            application/json:
                                schema:
                                    $ref: "#/components/schemas/Tree/properties/child"
                schemas:
                    Tree:
                        type: object
                        properties:
                            child:
                                $ref: "#/components/schemas/Tree"
                    Private:
                        type: string
            """,
            ObjectNode.class
        );
        final JsonNode visible = new OaComponents(() -> document).read().path("components");
        MatcherAssert.assertThat(
            "Recursive dependency traversal lost a visible component or retained an unused model",
            List.of(
                visible.path("responses").has("Branch"),
                visible.path("schemas").path("Tree").path("properties").has("child"),
                visible.path("schemas").has("Private")
            ),
            Matchers.contains(true, true, false)
        );
    }
}
