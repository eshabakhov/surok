/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * OpenAPI document encapsulating its parsed JSON representation.
 * For example, select operations without exposing or mutating the underlying tree.
 *
 * @since 0.0.1
 */
public final class OaJson implements OpenApi {

    /** Complete document, never modified by selection or serialization. */
    private final ObjectNode document;

    /** Pointer to the security schemes within components. */
    private final JsonPointer schemes;

    /**
     * New document backed by a parsed representation.
     *
     * @param document Parsed OpenAPI document
     */
    public OaJson(final ObjectNode document) {
        this(document, JsonPointer.compile("/securitySchemes"));
    }

    /**
     * New document with the security component location.
     *
     * @param document Parsed OpenAPI document
     * @param schemes Security scheme catalog pointer
     */
    private OaJson(final ObjectNode document, final JsonPointer schemes) {
        this.document = document;
        this.schemes = schemes;
    }

    @Override
    public Paths paths() {
        return new PtsYaml(this.document.path("paths"), this.document.path("security"));
    }

    @Override
    public Operations operations() {
        return new OpsCatalog(
            this.paths(),
            new OpsYaml("", this.document.path("x-operations"), this.document.path("security"))
        );
    }

    @Override
    public OpenApi selection(final Predicate<Operation> condition) {
        final ObjectNode selected = this.document.deepCopy();
        final ObjectNode paths = selected.withObjectProperty("paths");
        for (final Operation operation : this.operations().iterate()) {
            if (!condition.test(operation)) {
                if (operation.path().isEmpty()) {
                    selected.withArray("x-operations").removeIf(
                        item -> operation.identifier().equals(item.path("operationId").asText())
                    );
                } else {
                    paths.withObjectProperty(operation.path()).remove(
                        operation.method().name().toLowerCase(Locale.ROOT)
                    );
                }
            }
        }
        paths.properties().removeIf(
            entry -> entry.getKey().startsWith("/")
                && Arrays.stream(RequestMethod.values()).noneMatch(
                    method -> entry.getValue().has(method.name().toLowerCase(Locale.ROOT))
                )
        );
        return new OaJson(this.components(selected));
    }

    @Override
    public OpenApi relative(final String path) {
        final ObjectNode selected = this.document.deepCopy();
        if (!path.isEmpty() && "/".equals(selected.at("/servers/0/url").asText())) {
            selected.withObject("/servers/0").put("url", path);
        }
        return new OaJson(selected);
    }

    @Override
    public byte[] json() throws IOException {
        return Json.mapper().writeValueAsBytes(this.document);
    }

    @Override
    public byte[] yaml() throws IOException {
        return Yaml.mapper().writeValueAsBytes(this.document);
    }

    /*
     * Retain transitive component dependencies of the selected document.
     */
    private ObjectNode components(final ObjectNode source) {
        final JsonNode catalog = source.path("components");
        source.remove("components");
        final ObjectNode selected = source.objectNode();
        final Deque<JsonPointer> pending = new ArrayDeque<>(this.references(source).toList());
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
            source.set("components", selected);
        }
        return source;
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
