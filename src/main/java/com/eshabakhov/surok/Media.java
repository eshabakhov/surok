/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok;

import java.util.Map;

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
     * Adds result pagination to the media.
     *
     * @param name Field name
     * @param value Result pagination
     * @return Updated media
     */
    Media<T> with(String name, ResultPage value);

    /**
     * Adds representation attributes.
     *
     * @param values Representation attributes
     * @return Updated media
     */
    Media<T> attributes(Map<String, Object> values);

    /**
     * Sets a title format for the representation.
     *
     * @param format Title format
     * @return Updated media
     */
    Media<T> title(String format);

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
