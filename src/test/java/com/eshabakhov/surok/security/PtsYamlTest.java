/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import java.nio.charset.StandardCharsets;
import java.util.stream.StreamSupport;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

/**
 * OpenAPI paths expose route items independently of custom operations.
 * For example, x-operations never creates a synthetic HTTP path.
 *
 * @since 0.0.1
 */
final class PtsYamlTest {

    /**
     * Keeps empty path items while excluding extensions from the route collection.
     *
     * @throws Exception If the document cannot be loaded
     */
    @Test
    void enumeratesOnlyHttpPaths() throws Exception {
        MatcherAssert.assertThat(
            "The paths omitted a route or included an extension",
            StreamSupport.stream(
                new OaYaml(
                    new ByteArrayResource(
                        """
                        paths:
                            /школы: {}
                            /корпуса: {summary: Корпуса}
                            x-description: {summary: Расширение}
                        x-operations:
                            - {operationId: Users.add, x-roles: [ADMIN]}
                        """.getBytes(StandardCharsets.UTF_8)
                    )
                ).paths().iterate().spliterator(), false
            ).map(PathItem::template).toList(),
            Matchers.contains("/школы", "/корпуса")
        );
    }

    /** Rejects a path absent from the description. */
    @Test
    void rejectsUnknownPaths() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new OaYaml(
                new ByteArrayResource("paths: {}".getBytes(StandardCharsets.UTF_8))
            ).paths().path("/неизвестный"),
            "The paths resolved an undeclared route"
        );
    }
}
