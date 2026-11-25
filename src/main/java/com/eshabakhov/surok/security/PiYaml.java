/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * One OpenAPI path item and its inherited security requirements.
 * For example, retrieve methods while keeping shared parameters out of the operation list.
 *
 * @since 0.0.1
 */
public final class PiYaml implements PathItem {

    /** Route template. */
    private final String route;

    /** Path item with shared metadata and HTTP operations. */
    private final JsonNode document;

    /** Inherited security requirements. */
    private final JsonNode security;

    /**
     * New path item.
     *
     * @param route Route template
     * @param document Path item section
     * @param security Inherited security
     */
    public PiYaml(final String route, final JsonNode document, final JsonNode security) {
        this.route = route;
        this.document = document;
        this.security = security;
    }

    @Override
    public String template() {
        return this.route;
    }

    @Override
    public Operations operations() {
        return new OpsYaml(this.route, this.document, this.security);
    }
}
