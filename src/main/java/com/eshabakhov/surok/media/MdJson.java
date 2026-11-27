/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.media;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Printable;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JSON implementation of {@link Media} based on Jackson.
 *
 * @since 0.0.1
 */
public final class MdJson implements Media<ObjectNode> {

    /** Jackson object node being filled. */
    private final ObjectNode node;

    /** Names printed as attributes. */
    private final Set<String> attributes;

    /** Included fields. */
    private final Set<String> fields;

    /** Whether the included fields are restricted. */
    private final boolean restricted;

    /**
     * Creates an empty JSON media.
     */
    public MdJson() {
        this(
            new ObjectNode(JsonNodeFactory.instance), new LinkedHashSet<>(0),
            new LinkedHashSet<>(0), false
        );
    }

    /**
     * Creates JSON media with collected values.
     *
     * @param node Collected values
     * @param attributes Names printed as attributes
     * @param fields Included fields
     * @param restricted Whether fields are restricted
     */
    private MdJson(
        final ObjectNode node,
        final Set<String> attributes,
        final Set<String> fields,
        final boolean restricted
    ) {
        this.node = node;
        this.attributes = attributes;
        this.fields = fields;
        this.restricted = restricted;
    }

    @Override
    public MdJson attribute(final String name, final String value) {
        final JsonNode printed;
        if (value == null) {
            printed = JsonNodeFactory.instance.nullNode();
        } else {
            printed = JsonNodeFactory.instance.textNode(value);
        }
        return this.metadata(name, printed);
    }

    @Override
    public MdJson attribute(final String name, final Long value) {
        final JsonNode printed;
        if (value == null) {
            printed = JsonNodeFactory.instance.nullNode();
        } else {
            printed = JsonNodeFactory.instance.numberNode(value);
        }
        return this.metadata(name, printed);
    }

    @Override
    public MdJson attribute(final String name, final Integer value) {
        final JsonNode printed;
        if (value == null) {
            printed = JsonNodeFactory.instance.nullNode();
        } else {
            printed = JsonNodeFactory.instance.numberNode(value);
        }
        return this.metadata(name, printed);
    }

    @Override
    public MdJson attribute(final String name, final Boolean value) {
        final JsonNode printed;
        if (value == null) {
            printed = JsonNodeFactory.instance.nullNode();
        } else {
            printed = JsonNodeFactory.instance.booleanNode(value);
        }
        return this.metadata(name, printed);
    }

    @Override
    public MdJson with(final String name, final String value) {
        final ObjectNode copied = this.node.deepCopy();
        if (this.allowed(name)) {
            copied.put(name, value);
        }
        return new MdJson(
            copied, this.attributes, this.fields, this.restricted
        );
    }

    @Override
    public MdJson with(final String name, final Long value) {
        final ObjectNode copied = this.node.deepCopy();
        if (this.allowed(name)) {
            copied.put(name, value);
        }
        return new MdJson(
            copied, this.attributes, this.fields, this.restricted
        );
    }

    @Override
    public MdJson with(final String name, final Integer value) {
        final ObjectNode copied = this.node.deepCopy();
        if (this.allowed(name)) {
            copied.put(name, value);
        }
        return new MdJson(
            copied, this.attributes, this.fields, this.restricted
        );
    }

    @Override
    public MdJson with(final String name, final Boolean value) {
        final ObjectNode copied = this.node.deepCopy();
        if (this.allowed(name)) {
            copied.put(name, value);
        }
        return new MdJson(
            copied, this.attributes, this.fields, this.restricted
        );
    }

    @Override
    public MdJson with(final String name, final Printable value)
        throws Exception {
        final ObjectNode copied = this.node.deepCopy();
        final boolean allowed = this.allowed(name);
        final ObjectNode printed;
        if (allowed) {
            printed = value.print(new MdJson()).print();
        } else {
            printed = JsonNodeFactory.instance.objectNode();
        }
        if (
            allowed
                && printed.size() == 1
                && printed.elements().next().isArray()
        ) {
            copied.set(name, printed.elements().next());
        } else if (allowed) {
            copied.set(name, printed);
        }
        return new MdJson(
            copied, this.attributes, this.fields, this.restricted
        );
    }

    @Override
    public Media<ObjectNode> with(final Printable value) throws Exception {
        return value.print(this);
    }

    @Override
    public MdJson items(final String name, final Iterable<? extends Printable> values)
        throws Exception {
        final ObjectNode copied = this.node.deepCopy();
        if (this.allowed("items")) {
            final ArrayNode items = JsonNodeFactory.instance.arrayNode();
            for (final Printable value : values) {
                items.add(value.print(new MdJson()).print());
            }
            copied.set("items", items);
        }
        return new MdJson(copied, this.attributes, this.fields, this.restricted);
    }

    @Override
    public MdJson include(final String... names) {
        final Set<String> included = new LinkedHashSet<>(
            Arrays.asList(names)
        );
        final ObjectNode copied = this.node.deepCopy();
        copied.retain(included);
        return new MdJson(
            copied,
            this.attributes.stream().filter(included::contains)
                .collect(Collectors.toUnmodifiableSet()),
            Collections.unmodifiableSet(included),
            true
        );
    }

    @Override
    public ObjectNode print() {
        return this.node.deepCopy();
    }

    private MdJson metadata(final String name, final JsonNode value) {
        final ObjectNode copied = this.node.deepCopy();
        final Set<String> names = new LinkedHashSet<>(this.attributes);
        if (!this.restricted || this.fields.contains(name)) {
            if (copied.has(name) && !names.contains(name)) {
                throw new IllegalArgumentException(
                    String.format("Element and attribute share the name '%s'", name)
                );
            }
            copied.set(name, value);
            names.add(name);
        }
        return new MdJson(
            copied, Collections.unmodifiableSet(names), this.fields, this.restricted
        );
    }

    private boolean allowed(final String name) {
        final boolean allowed = !this.restricted || this.fields.contains(name);
        if (allowed && this.attributes.contains(name)) {
            throw new IllegalArgumentException(
                String.format("Element and attribute share the name '%s'", name)
            );
        }
        return allowed;
    }
}
