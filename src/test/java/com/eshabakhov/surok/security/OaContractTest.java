/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import com.eshabakhov.surok.school.building.Building;
import com.eshabakhov.surok.school.building.Buildings;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Verifies coverage of the published OpenAPI contract.
 * For example, each documented media type must have a schema and a sample body.
 *
 * @since 0.0.1
 */
final class OaContractTest {

    /**
     * Requires the domain action for each published Buildings operation.
     *
     * @throws Exception If the specification cannot be read
     */
    @Test
    void declaresBuildingActions() throws Exception {
        MatcherAssert.assertThat(
            "Buildings operations must declare the action enforced by the API",
            new OaYaml(new ClassPathResource("openapi.yml")).read().path("paths")
                .properties().stream().flatMap(
                    path -> path.getValue().properties().stream().filter(
                        method -> method.getValue().path("tags").valueStream()
                            .anyMatch(tag -> "Buildings".equals(tag.asText()))
                    ).map(
                        method -> String.join(
                            " ", method.getKey(), path.getKey(),
                            method.getValue().path("operationId").asText()
                        )
                    )
                ).toList(),
            Matchers.containsInAnyOrder(
                "get /schools/{school}/buildings Buildings.selection",
                "post /schools/{school}/buildings Buildings.create",
                "get /schools/{school}/buildings/{building} Buildings.building",
                "put /schools/{school}/buildings/{building} Building.revise",
                "delete /schools/{school}/buildings/{building} Buildings.remove",
                "get /schools/{school}/buildings/{building}/edit openBuildingEditForm",
                "get /schools/{school}/buildings/create openBuildingCreationForm"
            )
        );
    }

    /**
     * Detects a domain method rename that would leave the contract pointing to an obsolete name.
     *
     * @throws Exception If the contract cannot be read
     */
    @Test
    void bindsBuildingOperationIdsToDomainMethods() throws Exception {
        MatcherAssert.assertThat(
            "A Buildings operation references a missing domain method",
            new OaYaml(new ClassPathResource("openapi.yml")).read().path("paths").valueStream()
                .flatMap(JsonNode::valueStream)
                .filter(operation -> operation.has("x-permission"))
                .map(operation -> operation.path("operationId").asText())
                .filter(id -> id.startsWith("Buildings.") || id.startsWith("Building.")).toList(),
            Matchers.everyItem(
                Matchers.in(
                    Stream.of(Buildings.class, Building.class).flatMap(
                        type -> Arrays.stream(type.getDeclaredMethods()).map(
                            method -> String.join(".", type.getSimpleName(), method.getName())
                        )
                    ).toList()
                )
            )
        );
    }

    /**
     * Requires a typed example for every request and response representation.
     *
     * @throws Exception If the specification cannot be read
     */
    @Test
    void describesEveryDocumentedRepresentation() throws Exception {
        final JsonNode document = new OaYaml(new ClassPathResource("openapi.yml")).read();
        MatcherAssert.assertThat(
            "The API contract has representations without schemas or examples",
            document.path("paths").properties().stream().flatMap(
                path -> path.getValue().properties().stream().flatMap(
                    operation -> this.representations(document, path.getKey(), operation)
                )
            ).toList(),
            Matchers.empty()
        );
    }

    /**
     * Requires descriptions and explicit types for named payload fields.
     *
     * @throws Exception If the specification cannot be read
     */
    @Test
    void documentsTheFieldsOfEveryObjectSchema() throws Exception {
        MatcherAssert.assertThat(
            "An OpenAPI object or field lacks a description or type",
            new OaYaml(new ClassPathResource("openapi.yml")).read()
                .path("components").path("schemas").properties().stream()
                .flatMap(this::fields).toList(),
            Matchers.empty()
        );
    }

    /**
     * Keeps reusable examples and schemas available in the packaged document.
     *
     * @throws Exception If the specification cannot be read
     */
    @Test
    void resolvesEveryReusableExampleAndSchema() throws Exception {
        final JsonNode document = new OaYaml(new ClassPathResource("openapi.yml")).read();
        MatcherAssert.assertThat(
            "The API contract has references that cannot be resolved",
            document.findValuesAsText("$ref").stream().filter(
                ref -> !ref.startsWith("#/") || document.at(ref.substring(1)).isMissingNode()
            ).toList(),
            Matchers.empty()
        );
    }

    /*
     * Check request and response bodies for the operation.
     */
    private Stream<String> representations(
        final JsonNode document, final String path, final Map.Entry<String, JsonNode> operation
    ) {
        final String route = String.format("%s %s", operation.getKey(), path);
        return Stream.concat(
            this.media(document, operation.getValue().path("requestBody"), route),
            operation.getValue().path("responses").properties().stream().flatMap(
                response -> this.media(
                    document, response.getValue(), String.format("%s %s", route, response.getKey())
                )
            )
        );
    }

    /*
     * Resolve a reusable body and find representations with incomplete documentation.
     */
    private Stream<String> media(final JsonNode document, final JsonNode body, final String route) {
        final JsonNode value;
        if (body.has("$ref")) {
            value = document.at(body.path("$ref").asText().substring(1));
        } else {
            value = body;
        }
        return value.path("content").properties().stream().filter(
            media -> !media.getValue().has("schema") || !media.getValue().has("example")
                && media.getValue().path("examples").isEmpty()
        ).map(media -> String.format("%s %s", route, media.getKey()));
    }

    /*
     * Find object schemas and properties with incomplete documentation.
     */
    private Stream<String> fields(final Map.Entry<String, JsonNode> schema) {
        final JsonNode value = schema.getValue();
        return Stream.concat(
            Stream.of(schema.getKey()).filter(
                name -> value.path("description").asText().isBlank()
                    || "object".equals(value.path("type").asText())
                    && value.path("properties").isEmpty()
            ),
            value.path("properties").properties().stream().filter(
                field -> field.getValue().path("description").asText().isBlank()
                    || !field.getValue().has("type") && !field.getValue().has("allOf")
                    && !field.getValue().has("oneOf")
            ).map(field -> String.format("%s.%s", schema.getKey(), field.getKey()))
        );
    }
}
