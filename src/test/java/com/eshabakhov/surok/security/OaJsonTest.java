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
final class OaJsonTest {

    /**
     * Keeps an entire schema referenced through one of its properties.
     *
     * @throws Exception If the fixture cannot be read
     */
    @Test
    void followsNestedAndRecursiveComponentReferences() throws Exception {
        final JsonNode visible = Yaml.mapper().readTree(
            new OaJson(
                Yaml.mapper().readValue(
                    """
                    paths:
                        /дерево:
                            get:
                                operationId: Tree.read
                                x-roles: []
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
                )
            ).selection(operation -> true).yaml()
        ).path("components");
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

    /**
     * Selects typed operations without changing the source or shared path metadata.
     *
     * @throws Exception If the representation cannot be read
     */
    @Test
    void selectsOperationsWithoutChangingTheSource() throws Exception {
        final OpenApi origin = new OaJson(
            Yaml.mapper().readValue(
                """
                paths:
                    /школы:
                        parameters: [{name: limit, in: query}]
                        get:
                            operationId: Schools.read
                            x-roles: []
                        post:
                            operationId: Schools.create
                            x-roles: [ADMIN]
                    /закрыто:
                        delete:
                            operationId: Schools.remove
                            x-roles: [ADMIN]
                x-operations:
                    - {operationId: Users.add, x-roles: [ADMIN]}
                """,
                ObjectNode.class
            )
        );
        final OpenApi selected = origin.selection(
            operation -> "Schools.read".equals(operation.identifier())
        );
        MatcherAssert.assertThat(
            "Selection lost path metadata, exposed denied operations or changed the source",
            List.of(
                Yaml.mapper().readTree(selected.json()).at("/paths/~1школы/parameters/0/name")
                    .asText(),
                selected.operations().iterate().iterator().next().identifier(),
                String.valueOf(Yaml.mapper().readTree(selected.yaml()).path("paths").size()),
                String.valueOf(Yaml.mapper().readTree(selected.json()).path("x-operations").size()),
                String.valueOf(Yaml.mapper().readTree(origin.json()).at("/paths/~1школы").size())
            ),
            Matchers.contains("limit", "Schools.read", "1", "0", "3")
        );
    }

    /**
     * Preserves arbitrary OpenAPI fields in both formats while resolving the root server.
     *
     * @throws Exception If either representation cannot be read
     */
    @Test
    void preservesTheDocumentWhenResolvingItsServer() throws Exception {
        final ObjectNode document = Yaml.mapper().readValue(
            """
            openapi: 3.1.0
            servers:
                - url: "/"
                - url: https://example.org
            paths: {}
            components:
                schemas:
                    Sample:
                        example: [1.25, null, true, Пример]
            x-description: {enabled: false}
            """,
            ObjectNode.class
        );
        final OpenApi origin = new OaJson(document);
        final OpenApi selected = origin.relative("/школа");
        final ObjectNode expected = document.deepCopy();
        expected.withObject("/servers/0").put("url", "/школа");
        MatcherAssert.assertThat(
            "Server resolution changed source data or lost fields in one representation",
            List.of(
                Yaml.mapper().readTree(origin.json()),
                Yaml.mapper().readTree(selected.json()),
                Yaml.mapper().readTree(selected.yaml())
            ),
            Matchers.contains(document, expected, expected)
        );
    }
}
