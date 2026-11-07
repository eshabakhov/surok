/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Yaml;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.Resource;

/**
 * OpenAPI description stored as a YAML resource.
 * For example, read the private classpath resource openapi.yml for each request.
 *
 * @since 0.0.1
 */
public final class OaYaml implements ApiDescription {

    /** Canonical description. */
    private final Resource source;

    /**
     * New description from YAML.
     *
     * @param source Canonical resource
     */
    public OaYaml(final Resource source) {
        this.source = source;
    }

    @Override
    public ObjectNode read() throws IOException {
        try (InputStream stream = this.source.getInputStream()) {
            return Yaml.mapper().readValue(stream, ObjectNode.class);
        }
    }
}
