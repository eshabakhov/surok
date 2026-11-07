/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A description with only the components reachable from its published operations.
 * For example, wrap an authorized description to omit administrator models for readers.
 *
 * @since 0.0.1
 */
final class OaComponents implements ApiDescription {

    /** Description whose operations have already been filtered. */
    private final ApiDescription origin;

    /** Pointer to the security scheme catalog within components. */
    private final JsonPointer schemes;

    /**
     * New description with reachable components.
     *
     * @param origin Source of an independent filtered document
     */
    OaComponents(final ApiDescription origin) {
        this(origin, JsonPointer.compile("/securitySchemes"));
    }

    /**
     * New description with a pointer to the scheme catalog.
     *
     * @param origin Source of an independent filtered document
     * @param schemes Pointer relative to the components section
     */
    private OaComponents(final ApiDescription origin, final JsonPointer schemes) {
        this.origin = origin;
        this.schemes = schemes;
    }

    @Override
    public ObjectNode read() throws IOException {
        final ObjectNode document = this.origin.read();
        final JsonNode catalog = document.path("components");
        document.remove("components");
        final ObjectNode selected = document.objectNode();
        final Deque<JsonPointer> pending = new ArrayDeque<>(this.references(document).toList());
        while (!pending.isEmpty()) {
            final JsonPointer reference = pending.removeFirst();
            if (selected.at(reference).isMissingNode()) {
                final JsonNode component = catalog.at(reference);
                if (!component.isMissingNode()) {
                    selected.withObjectProperty(reference.getMatchingProperty())
                        .set(reference.tail().getMatchingProperty(), component);
                    this.references(component).forEach(pending::addLast);
                }
            }
        }
        if (!selected.isEmpty()) {
            document.set("components", selected);
        }
        return document;
    }

    /*
     * Follow local references and the named schemes of security requirements.
     */
    private Stream<JsonPointer> references(final JsonNode node) {
        return Stream.concat(
            node.findValuesAsText("$ref").stream()
                .filter(ref -> ref.startsWith("#/components/"))
                .map(ref -> JsonPointer.compile(URI.create(ref).getFragment()).tail())
                .filter(ref -> !ref.tail().matches()).map(
                    ref -> JsonPointer.empty().appendProperty(ref.getMatchingProperty())
                        .appendProperty(ref.tail().getMatchingProperty())
                ),
            node.findValues("security").stream().filter(JsonNode::isArray)
                .flatMap(array -> StreamSupport.stream(array.spliterator(), false))
                .filter(JsonNode::isObject)
                .flatMap(requirement -> requirement.properties().stream())
                .map(Map.Entry::getKey)
                .map(this.schemes::appendProperty)
        ).distinct();
    }
}
