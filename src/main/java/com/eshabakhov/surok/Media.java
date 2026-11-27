/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok;

/**
 * Media that an object can print itself into.
 * Implementations define how data is rendered (JSON, XML, or HTML).
 *
 * @param <T> Printed representation type
 * @since 0.0.1
 */
@SuppressWarnings("PMD.TooManyMethods")
public interface Media<T> {

    /**
     * Adds string metadata: an XML attribute or a JSON property.
     * Attribute and element names must not collide at the same level.
     *
     * @param name Attribute name
     * @param value Attribute value
     * @return Updated media
     */
    default Media<T> attribute(final String name, final String value) {
        return this.with(name, value);
    }

    /**
     * Adds long metadata while preserving its JSON number type.
     *
     * @param name Attribute name
     * @param value Attribute value
     * @return Updated media
     */
    default Media<T> attribute(final String name, final Long value) {
        return this.with(name, value);
    }

    /**
     * Adds integer metadata while preserving its JSON number type.
     *
     * @param name Attribute name
     * @param value Attribute value
     * @return Updated media
     */
    default Media<T> attribute(final String name, final Integer value) {
        return this.with(name, value);
    }

    /**
     * Adds boolean metadata while preserving its JSON boolean type.
     *
     * @param name Attribute name
     * @param value Attribute value
     * @return Updated media
     */
    default Media<T> attribute(final String name, final Boolean value) {
        return this.with(name, value);
    }

    /**
     * Adds a string value to the media.
     *
     * @param name  Field name
     * @param value String value
     * @return Updated media
     */
    Media<T> with(String name, String value);

    /**
     * Adds a long value to the media.
     *
     * @param name  Field name
     * @param value Long value
     * @return Updated media
     */
    Media<T> with(String name, Long value);

    /**
     * Adds an integer value to the media.
     *
     * @param name  Field name
     * @param value Integer value
     * @return Updated media
     */
    Media<T> with(String name, Integer value);

    /**
     * Adds a boolean value to the media.
     *
     * @param name Field name
     * @param value Boolean value
     * @return Updated media
     */
    Media<T> with(String name, Boolean value);

    /**
     * Adds a printable object to the media.
     *
     * @param name Field name
     * @param value Printable object
     * @return Updated media
     * @throws Exception If the object cannot be printed
     */
    Media<T> with(String name, Printable value) throws Exception;

    /**
     * Adds a printable object to the current media level.
     *
     * @param value Printable object
     * @return Updated media
     * @throws Exception If the object cannot be printed
     */
    Media<T> with(Printable value) throws Exception;

    /**
     * Prints collection elements at the current level, including an empty collection.
     * XML uses the supplied element name; JSON exposes the collection as an items array.
     * For example, a named parent can embed {@code new PrItems("class", classes)}.
     *
     * @param name Singular element name
     * @param values Collection elements
     * @return Updated media
     * @throws Exception If an element cannot be printed
     */
    default Media<T> items(final String name, final Iterable<? extends Printable> values)
        throws Exception {
        Media<T> result = this;
        for (final Printable value : values) {
            result = result.with(name, value);
        }
        return result;
    }

    /**
     * Includes specified fields.
     *
     * @param name Fields name
     * @return Updated media
     */
    Media<T> include(String... name);

    /**
     * Prints the collected values.
     *
     * @return Printed representation
     */
    T print();
}
