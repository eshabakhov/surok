/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Yaml;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Predicate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * OpenAPI description stored as a YAML resource.
 * For example, read the private classpath resource openapi.yml for each request.
 *
 * @since 0.0.1
 */
public final class OaYaml implements OpenApi {

    /** Canonical description. */
    private final Resource source;

    /**
     * New description from the packaged OpenAPI resource.
     */
    public OaYaml() {
        this(new ClassPathResource("openapi.yml"));
    }

    /**
     * New description from YAML.
     *
     * @param source Canonical resource
     */
    public OaYaml(final Resource source) {
        this.source = source;
    }

    @Override
    public Paths paths() throws IOException {
        return this.description().paths();
    }

    @Override
    public Operations operations() throws IOException {
        return this.description().operations();
    }

    @Override
    public OpenApi selection(final Predicate<Operation> condition) throws IOException {
        return this.description().selection(condition);
    }

    @Override
    public OpenApi relative(final String path) throws IOException {
        return this.description().relative(path);
    }

    @Override
    public byte[] json() throws IOException {
        return this.description().json();
    }

    @Override
    public byte[] yaml() throws IOException {
        return this.description().yaml();
    }

    /*
     * Load an independent document while closing the resource stream.
     */
    private OpenApi description() throws IOException {
        try (InputStream stream = this.source.getInputStream()) {
            return new OaJson(Yaml.mapper().readValue(stream, ObjectNode.class));
        }
    }
}
