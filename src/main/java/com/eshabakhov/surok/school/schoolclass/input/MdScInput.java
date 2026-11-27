/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.school.schoolclass.input;

import com.eshabakhov.surok.Media;
import com.eshabakhov.surok.Printable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable map medium for school class input.
 *
 * @since 0.0.1
 */
public final class MdScInput implements Media<Map<String, String>> {

    /** Collected input fields. */
    private final Map<String, String> fields;

    /**
     * New empty input medium.
     */
    public MdScInput() {
        this(Collections.emptyMap());
    }

    /**
     * New input medium.
     *
     * @param fields Collected fields
     */
    private MdScInput(final Map<String, String> fields) {
        this.fields = fields;
    }

    @Override
    public MdScInput with(final String name, final String value) {
        final Map<String, String> changed = new LinkedHashMap<>(this.fields);
        changed.put(name, value);
        return new MdScInput(Collections.unmodifiableMap(changed));
    }

    @Override
    public MdScInput with(final String name, final Long value) {
        String printed = null;
        if (value != null) {
            printed = value.toString();
        }
        return this.with(name, printed);
    }

    @Override
    public MdScInput with(final String name, final Integer value) {
        String printed = null;
        if (value != null) {
            printed = value.toString();
        }
        return this.with(name, printed);
    }

    @Override
    public MdScInput with(final String name, final Boolean value) {
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
    public MdScInput include(final String... names) {
        return this;
    }

    @Override
    public Map<String, String> print() {
        return this.fields;
    }
}
