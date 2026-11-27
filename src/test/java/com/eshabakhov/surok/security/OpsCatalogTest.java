/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

/**
 * Operation lookup combines HTTP paths and internal declarations.
 * For example, duplicate identifiers are rejected across both sources.
 *
 * @since 0.0.1
 */
final class OpsCatalogTest {

    /** Rejects duplicate identifiers before a domain operation can resolve the wrong rule. */
    @Test
    void rejectsDuplicateOperationIdentifiers() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new OaYaml(
                new ByteArrayResource(
                    """
                    paths:
                        /школы/{school}/корпуса:
                            get:
                                operationId: Buildings.create
                                x-roles: [STUDENT]
                            post:
                                operationId: Buildings.create
                                x-roles: [ADMIN]
                    """.getBytes(StandardCharsets.UTF_8)
                )
            ).operations().iterate(),
            "A duplicate identifier entered the operation catalog"
        );
    }

    /**
     * Finds internal operations without adding them to HTTP paths.
     *
     * @throws Exception If the document cannot be read
     */
    @Test
    void resolvesInternalOperationsByIdentifier() throws Exception {
        MatcherAssert.assertThat(
            "An internal operation disappeared from the catalog",
            new OaYaml(
                new ByteArrayResource(
                    """
                    paths: {}
                    x-operations:
                        - {operationId: Users.add, x-roles: [ADMIN]}
                    """.getBytes(StandardCharsets.UTF_8)
                )
            ).operations().operation("Users.add").identifier(),
            Matchers.is("Users.add")
        );
    }

    /** Rejects a collision between HTTP and internal operations. */
    @Test
    void rejectsIdentifiersSharedWithInternalOperations() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new OaYaml(
                new ByteArrayResource(
                    """
                    paths:
                        /пользователи:
                            post: {operationId: Users.add, x-roles: [ADMIN]}
                    x-operations:
                        - {operationId: Users.add, x-roles: [ADMIN]}
                    """.getBytes(StandardCharsets.UTF_8)
                )
            ).operations().iterate(),
            "The catalog accepted an ambiguous identifier across its sources"
        );
    }
}
