/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.media;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Printable;
import com.eshabakhov.surok.ResultPage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * JSON implementation of {@link Media} based on Jackson.
 *
 * @since 0.0.1
 */
public final class MdJson implements Media<ObjectNode> {

    /** Jackson object node being filled. */
    private final ObjectNode node;

    /** Included fields. */
    private final Set<String> fields;

    /** Whether the included fields are restricted. */
    private final boolean restricted;

    /**
     * Creates an empty JSON media.
     */
    public MdJson() {
        this(new ObjectNode(JsonNodeFactory.instance), new LinkedHashSet<>(0), false);
    }

    /**
     * Creates JSON media with collected values.
     *
     * @param node Collected values
     * @param fields Included fields
     * @param restricted Whether fields are restricted
     */
    private MdJson(
        final ObjectNode node,
        final Set<String> fields,
        final boolean restricted
    ) {
        this.node = node;
        this.fields = fields;
        this.restricted = restricted;
    }

    @Override
    public MdJson with(final String name, final String value) {
        final ObjectNode copied = this.node.deepCopy();
        if (this.allowed(name)) {
            copied.put(name, value);
        }
        return new MdJson(
            copied, this.fields, this.restricted
        );
    }

    @Override
    public MdJson with(final String name, final Long value) {
        final ObjectNode copied = this.node.deepCopy();
        if (this.allowed(name)) {
            copied.put(name, value);
        }
        return new MdJson(
            copied, this.fields, this.restricted
        );
    }

    @Override
    public MdJson with(final String name, final Integer value) {
        final ObjectNode copied = this.node.deepCopy();
        if (this.allowed(name)) {
            copied.put(name, value);
        }
        return new MdJson(
            copied, this.fields, this.restricted
        );
    }

    @Override
    public MdJson with(final String name, final Boolean value) {
        final ObjectNode copied = this.node.deepCopy();
        if (this.allowed(name)) {
            copied.put(name, value);
        }
        return new MdJson(
            copied, this.fields, this.restricted
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
            copied, this.fields, this.restricted
        );
    }

    @Override
    public MdJson with(final Printable value) throws Exception {
        final ObjectNode copied = this.node.deepCopy();
        final ArrayNode items = JsonNodeFactory.instance.arrayNode();
        final ObjectNode printed = value.print(new MdJson()).print();
        if (!printed.isEmpty()) {
            final JsonNode selected = printed.elements().next();
            if (selected.isArray()) {
                items.addAll((ArrayNode) selected);
            } else {
                items.add(selected);
            }
        }
        if (this.allowed("items")) {
            copied.set("items", items);
        }
        return new MdJson(
            copied, this.fields, this.restricted
        );
    }

    @Override
    public MdJson with(final String name, final ResultPage value) {
        final ObjectNode copied = this.node.deepCopy();
        if (this.allowed(name)) {
            copied.set(
                name,
                JsonNodeFactory.instance.objectNode()
                    .put("limit", value.limit())
                    .put("offset", value.offset())
                    .put("total", value.total())
                    .put("totalPages", value.totalPages())
                    .put("hasNext", value.hasNext())
                    .put("hasPrev", value.hasPrev())
            );
        }
        return new MdJson(
            copied, this.fields, this.restricted
        );
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
        return new MdJson(copied, this.fields, this.restricted);
    }

    @Override
    public MdJson attributes(final Map<String, Object> values) {
        return this;
    }

    @Override
    public MdJson title(final String format) {
        return this;
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
            Collections.unmodifiableSet(included),
            true
        );
    }

    @Override
    public ObjectNode print() {
        return this.node.deepCopy();
    }

    private boolean allowed(final String name) {
        return !this.restricted || this.fields.contains(name);
    }
}
