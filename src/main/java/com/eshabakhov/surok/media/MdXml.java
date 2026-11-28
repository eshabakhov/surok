/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.media;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Printable;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;

/**
 * XML implementation of {@link Media}.
 *
 * @since 0.0.1
 */
public final class MdXml implements Media<String> {

    /** Characters outside the XML 1.0 Char production. */
    private static final Pattern INVALID = Pattern.compile(
        "[^\\x09\\x0A\\x0D\\x20-\\x{D7FF}\\x{E000}-\\x{FFFD}\\x{10000}-\\x{10FFFF}]"
    );

    /** XML root element. */
    private final String root;

    /** Collected XML elements. */
    private final Map<String, String> elements;

    /** Included fields. */
    private final Set<String> fields;

    /** Whether the included fields are restricted. */
    private final boolean restricted;

    /** Escaped attributes of the root element. */
    private final Map<String, String> attributes;

    /**
     * New XML media with the specified root element.
     *
     * @param root Root element
     */
    public MdXml(final String root) {
        this(
            root,
            new LinkedHashMap<>(0),
            new LinkedHashSet<>(0),
            false,
            new LinkedHashMap<>(0)
        );
    }

    /**
     * New XML media with collected values.
     *
     * @param root Root element
     * @param elements Collected XML elements
     * @param fields Included fields
     * @param restricted Whether fields are restricted
     * @param attributes Escaped root attributes
     */
    private MdXml(
        final String root,
        final Map<String, String> elements,
        final Set<String> fields,
        final boolean restricted,
        final Map<String, String> attributes
    ) {
        this.root = root;
        this.elements = elements;
        this.fields = fields;
        this.restricted = restricted;
        this.attributes = attributes;
    }

    @Override
    public MdXml attribute(final String name, final String value) {
        final Map<String, String> copied = new LinkedHashMap<>(this.attributes);
        if (!this.restricted || this.fields.contains(name)) {
            if (this.elements.containsKey(name)) {
                throw new IllegalArgumentException(
                    String.format("Element and attribute share the name '%s'", name)
                );
            }
            String printed = "";
            if (value != null) {
                printed = MdXml.escape(value)
                    .replace("\t", "&#9;")
                    .replace(String.valueOf('\n'), "&#10;");
            }
            copied.put(name, printed);
        }
        return new MdXml(
            this.root, this.elements, this.fields, this.restricted,
            Collections.unmodifiableMap(copied)
        );
    }

    @Override
    public MdXml attribute(final String name, final Long value) {
        String printed = null;
        if (value != null) {
            printed = value.toString();
        }
        return this.attribute(name, printed);
    }

    @Override
    public MdXml attribute(final String name, final Integer value) {
        String printed = null;
        if (value != null) {
            printed = value.toString();
        }
        return this.attribute(name, printed);
    }

    @Override
    public MdXml attribute(final String name, final Boolean value) {
        String printed = null;
        if (value != null) {
            printed = value.toString();
        }
        return this.attribute(name, printed);
    }

    @Override
    public MdXml with(final String name, final String value) {
        final Map<String, String> copied = new LinkedHashMap<>(this.elements);
        if (this.allowed(name)) {
            final String element;
            if (value == null) {
                element = String.format(
                    "<%s xmlns:xsi=\"%s\" xsi:nil=\"true\"/>", name,
                    XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI
                );
            } else {
                element = String.format("<%1$s>%2$s</%1$s>", name, MdXml.escape(value));
            }
            copied.put(name, element);
        }
        return new MdXml(
            this.root,
            Collections.unmodifiableMap(copied),
            this.fields,
            this.restricted,
            this.attributes
        );
    }

    @Override
    public MdXml with(final String name, final Long value) {
        String printed = "";
        if (value != null) {
            printed = value.toString();
        }
        return this.with(name, printed);
    }

    @Override
    public MdXml with(final String name, final Integer value) {
        String printed = "";
        if (value != null) {
            printed = value.toString();
        }
        return this.with(name, printed);
    }

    @Override
    public MdXml with(final String name, final Boolean value) {
        String printed = "";
        if (value != null) {
            printed = value.toString();
        }
        return this.with(name, printed);
    }

    @Override
    public MdXml with(final String name, final Printable value)
        throws Exception {
        final Map<String, String> copied = new LinkedHashMap<>(this.elements);
        if (this.allowed(name)) {
            copied.merge(
                name,
                value.print(new MdXml(name)).print(),
                String::concat
            );
        }
        return new MdXml(
            this.root,
            Collections.unmodifiableMap(copied),
            this.fields,
            this.restricted,
            this.attributes
        );
    }

    @Override
    public MdXml with(final Printable value) throws Exception {
        return (MdXml) value.print(this);
    }

    @Override
    public MdXml include(final String... names) {
        final Set<String> included = new LinkedHashSet<>(
            Arrays.asList(names)
        );
        final Map<String, String> copied = new LinkedHashMap<>(this.elements);
        copied.keySet().retainAll(included);
        final Map<String, String> selected = new LinkedHashMap<>(this.attributes);
        selected.keySet().retainAll(included);
        return new MdXml(
            this.root,
            Collections.unmodifiableMap(copied),
            Collections.unmodifiableSet(included),
            true,
            Collections.unmodifiableMap(selected)
        );
    }

    @Override
    public String print() {
        final StringBuilder opening = new StringBuilder(this.root);
        this.attributes.forEach(
            (name, value) -> opening.append(' ').append(name).append("=\"")
                .append(value).append('"')
        );
        return String.format(
            "<%s>%s</%s>", opening, String.join("", this.elements.values()), this.root
        );
    }

    /*
     * Escape XML markup and preserve carriage returns across parser normalization.
     */
    private static String escape(final String value) {
        if (MdXml.INVALID.matcher(value).find()) {
            throw new IllegalArgumentException("Value contains a character forbidden in XML 1.0");
        }
        return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
            .replace(String.valueOf('\r'), "&#13;");
    }

    private boolean allowed(final String name) {
        final boolean allowed = !this.restricted || this.fields.contains(name);
        if (allowed && this.attributes.containsKey(name)) {
            throw new IllegalArgumentException(
                String.format("Element and attribute share the name '%s'", name)
            );
        }
        return allowed;
    }
}
