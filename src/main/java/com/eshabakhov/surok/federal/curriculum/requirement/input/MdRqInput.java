/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.federal.curriculum.requirement.input;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Printable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable map medium for federal curriculum requirement input.
 *
 * @since 0.0.1
 */
public final class MdRqInput implements Media<Map<String, String>> {

    /** Collected input fields. */
    private final Map<String, String> fields;

    /**
     * New empty input medium.
     */
    public MdRqInput() {
        this(Collections.emptyMap());
    }

    /**
     * New input medium.
     *
     * @param fields Collected fields
     */
    private MdRqInput(final Map<String, String> fields) {
        this.fields = fields;
    }

    @Override
    public MdRqInput with(final String name, final String value) {
        final Map<String, String> changed = new LinkedHashMap<>(this.fields);
        changed.put(name, value);
        return new MdRqInput(Collections.unmodifiableMap(changed));
    }

    @Override
    public MdRqInput with(final String name, final Long value) {
        String printed = null;
        if (value != null) {
            printed = value.toString();
        }
        return this.with(name, printed);
    }

    @Override
    public MdRqInput with(final String name, final Integer value) {
        String printed = null;
        if (value != null) {
            printed = value.toString();
        }
        return this.with(name, printed);
    }

    @Override
    public MdRqInput with(final String name, final Boolean value) {
        String printed = null;
        if (value != null) {
            printed = value.toString();
        }
        return this.with(name, printed);
    }

    @Override
    public Media<Map<String, String>> with(
        final String name,
        final Printable value
    ) throws Exception {
        return value.print(this);
    }

    @Override
    public Media<Map<String, String>> with(final Printable value)
        throws Exception {
        return value.print(this);
    }

    @Override
    public MdRqInput include(final String... names) {
        return this;
    }

    @Override
    public Map<String, String> print() {
        return this.fields;
    }
}
