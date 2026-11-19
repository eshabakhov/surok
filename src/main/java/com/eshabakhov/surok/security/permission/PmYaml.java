/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security.permission;

import com.eshabakhov.surok.security.ApiDescription;
import com.eshabakhov.surok.security.OaYaml;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Reads permission declarations from the canonical OpenAPI document.
 * For example, a Buildings POST contributes one operation for all its request formats.
 *
 * @since 0.0.1
 */
public final class PmYaml {

    /** Catalog shared by the packaged application and its domain decorators. */
    public static final PmYaml PACKAGED = PmYaml.packaged();

    /** Compiled domain operations. */
    private final PmGrants grants;

    /** Compiled endpoint bindings. */
    private final Map<String, Map<RequestMethod, PmBuilding>> declared;

    /**
     * Compile a catalog from one OpenAPI document.
     *
     * @param source OpenAPI source
     * @throws IOException If the contract cannot be read
     */
    PmYaml(final ApiDescription source) throws IOException {
        this(source.read());
    }

    /**
     * New catalog from the canonical document.
     *
     * @param document OpenAPI document
     */
    private PmYaml(final JsonNode document) {
        this(new PmGrants(document.path("paths")), document.path("paths"));
    }

    /**
     * New index binding routes to compiled grants.
     *
     * @param grants Operation permissions
     * @param paths OpenAPI paths
     */
    private PmYaml(final PmGrants grants, final JsonNode paths) {
        this(grants, PmYaml.bind(paths, grants));
    }

    /**
     * New catalog with resolved bindings.
     *
     * @param grants Operation permissions
     * @param declared Endpoint bindings
     */
    private PmYaml(
        final PmGrants grants, final Map<String, Map<RequestMethod, PmBuilding>> declared
    ) {
        this.grants = grants;
        this.declared = declared;
    }

    /**
     * Permission required by a Buildings operation.
     *
     * @param id OpenAPI operationId
     * @return Shared authorization rule
     */
    public Permission forOperation(final String id) {
        return this.grants.forOperation(id);
    }

    /**
     * Read the compiled endpoint bindings.
     *
     * @return Immutable endpoint index
     */
    Map<String, Map<RequestMethod, PmBuilding>> routes() {
        return this.declared;
    }

    private static PmYaml packaged() {
        try {
            return new PmYaml(new OaYaml(new ClassPathResource("openapi.yml")));
        } catch (final IOException failure) {
            throw new UncheckedIOException("Could not read the permission catalog", failure);
        }
    }

    private static Map<String, Map<RequestMethod, PmBuilding>> bind(
        final JsonNode paths, final PmGrants grants
    ) {
        final Map<String, Map<RequestMethod, PmBuilding>> routes = new LinkedHashMap<>();
        for (final Map.Entry<String, JsonNode> path
            : paths.properties()) {
            final Map<RequestMethod, PmBuilding> methods =
                new EnumMap<>(RequestMethod.class);
            for (final RequestMethod method : RequestMethod.values()) {
                final JsonNode operation = path.getValue().path(
                    method.name().toLowerCase(Locale.ROOT)
                );
                if (operation.has("x-permission") || operation.path("tags").valueStream().anyMatch(
                    tag -> "Buildings".equals(tag.asText())
                )) {
                    methods.put(method, PmYaml.resolve(path.getKey(), method, operation, grants));
                }
            }
            if (!methods.isEmpty()) {
                routes.put(path.getKey(), Collections.unmodifiableMap(methods));
            }
        }
        return Collections.unmodifiableMap(routes);
    }

    /*
     * Bind the documented action to its domain right and collect accepted body formats.
     */
    private static PmBuilding resolve(
        final String path, final RequestMethod method, final JsonNode operation,
        final PmGrants grants
    ) {
        if (!operation.has("x-permission")
            || !Arrays.asList(path.split("/")).contains("{school}")) {
            throw new IllegalArgumentException(
                String.format("Invalid Buildings permission binding for %s %s", method, path)
            );
        }
        final String id = operation.path("operationId").asText();
        return new PmBuilding(
            path, id, grants.forOperation(id),
            String.join(
                ", ", operation.path("requestBody").path("content").propertyStream()
                    .map(Map.Entry::getKey).toList()
            )
        );
    }
}
