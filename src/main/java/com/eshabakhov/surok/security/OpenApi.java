/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import java.io.IOException;
import java.util.function.Predicate;

/**
 * OpenAPI document with typed operations and independent representations.
 * For example, select permitted operations before publishing JSON or YAML.
 *
 * @since 0.0.1
 */
public interface OpenApi {

    /**
     * Access the HTTP paths section.
     *
     * @return Declared path items
     * @throws IOException If the source cannot be read
     */
    Paths paths() throws IOException;

    /**
     * Access HTTP and internal operations by their identifiers.
     *
     * @return Operations in this description
     * @throws IOException If the source cannot be read
     */
    Operations operations() throws IOException;

    /**
     * Select operations and their reachable components.
     *
     * @param condition Operation visibility
     * @return Independent selected description
     * @throws IOException If the source cannot be read
     */
    OpenApi selection(Predicate<Operation> condition) throws IOException;

    /**
     * Resolve the default server against an application context.
     *
     * @param path Application context path
     * @return Independent description with the resolved server
     * @throws IOException If the source cannot be read
     */
    OpenApi relative(String path) throws IOException;

    /**
     * Print the complete JSON representation.
     *
     * @return UTF-8 JSON
     * @throws IOException If the document cannot be printed
     */
    byte[] json() throws IOException;

    /**
     * Print the complete YAML representation.
     *
     * @return UTF-8 YAML
     * @throws IOException If the document cannot be printed
     */
    byte[] yaml() throws IOException;
}
