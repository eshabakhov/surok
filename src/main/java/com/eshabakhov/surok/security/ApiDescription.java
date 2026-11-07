/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;

/**
 * Source of an independent OpenAPI document.
 * For example, read a packaged YAML document before filtering its paths.
 *
 * @since 0.0.1
 */
@FunctionalInterface
public interface ApiDescription {

    /**
     * Read an independent document.
     *
     * @return OpenAPI document tree
     * @throws IOException If the document cannot be read
     */
    ObjectNode read() throws IOException;
}
