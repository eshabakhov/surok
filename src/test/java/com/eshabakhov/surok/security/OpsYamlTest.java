/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * OpenAPI operations expose validated role declarations.
 * For example, invalid roles cannot silently turn into public access.
 *
 * @since 0.0.1
 */
final class OpsYamlTest {

    /**
     * Finds an operation without interpreting path metadata as an operation.
     *
     * @throws Exception If the document cannot be read
     */
    @Test
    void selectsAnOperationByItsHttpBinding() throws Exception {
        MatcherAssert.assertThat(
            "The HTTP binding selected an incorrect operation",
            new OaYaml(
                new ByteArrayResource(
                    """
                    paths:
                        /школы/{school}/корпуса:
                            parameters: []
                            post:
                                operationId: Buildings.create
                                x-roles: [DIRECTOR]
                            get:
                                operationId: Buildings.selection
                                x-roles: [STUDENT]
                    """.getBytes(StandardCharsets.UTF_8)
                )
            ).operations().operation("/школы/{school}/корпуса", RequestMethod.POST).identifier(),
            Matchers.is("Buildings.create")
        );
    }

    /**
     * Rejects missing or malformed permission declarations.
     *
     * @param declaration Invalid declaration
     */
    @ParameterizedTest
    @ValueSource(
        strings = {
            "operationId: 42, x-roles: [ADMIN]", "operationId: '', x-roles: [ADMIN]",
            "x-roles: [ADMIN]", "operationId: Buildings.create",
            "operationId: Buildings.create, x-roles: null",
            "operationId: Buildings.create, x-roles: ADMIN",
            "operationId: Buildings.create, x-roles: [UNKNOWN]",
            "operationId: Buildings.create, x-roles: [42]",
            "operationId: Buildings.create, x-roles: {schoolRoles: [DIRECTOR]}"
        }
    )
    void rejectsInvalidDeclarations(final String declaration) {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new OaYaml(
                new ByteArrayResource(
                    String.format(
                        """
                        paths:
                            /школы/{school}/корпуса:
                                post: {%s}
                        """, declaration
                    ).getBytes(StandardCharsets.UTF_8)
                )
            ).operations().iterate(),
            "An invalid declaration entered the permission catalog"
        );
    }
}
