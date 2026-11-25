/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Paths parsed from the OpenAPI paths section.
 * For example, metadata extensions remain outside the collection of HTTP routes.
 *
 * @since 0.0.1
 */
public final class PtsYaml implements Paths {

    /** Paths section. */
    private final JsonNode document;

    /** Inherited security requirements. */
    private final JsonNode security;

    /**
     * New paths with root security requirements.
     *
     * @param document Paths section
     * @param security Inherited security
     */
    public PtsYaml(final JsonNode document, final JsonNode security) {
        this.document = document;
        this.security = security;
    }

    @Override
    public Iterable<PathItem> iterate() {
        return this.document.properties().stream()
            .filter(entry -> entry.getKey().startsWith("/"))
            .<PathItem>map(entry -> this.path(entry.getKey())).toList();
    }

    @Override
    public PathItem path(final String template) {
        if (!template.startsWith("/") || !this.document.has(template)) {
            throw new IllegalArgumentException(String.format("Unknown API path: %s", template));
        }
        return new PiYaml(template, this.document.path(template), this.security);
    }
}
