/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok;

import java.util.List;
import java.util.Map;

/**
 * Existing representations composed with numeric fields and named child objects.
 * For example, {@code new PrFields(List.of(subject), Map.of("hours", 1), Map.of())}
 * prints a subject alongside its hours without introducing a subject-specific wrapper.
 *
 * @since 0.0.1
 */
public final class PrFields implements Printable {

    /** Original representations printed at the same level. */
    private final List<? extends Printable> origins;

    /** Additional numeric fields. */
    private final Map<String, Integer> numbers;

    /** Nested representations. */
    private final Map<String, ? extends Printable> children;

    /**
     * New composed representation.
     *
     * @param origins Original objects
     * @param numbers Additional numeric fields
     * @param children Named child objects
     */
    public PrFields(final List<? extends Printable> origins, final Map<String, Integer> numbers,
        final Map<String, ? extends Printable> children) {
        this.origins = origins;
        this.numbers = numbers;
        this.children = children;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        Media<T> result = media;
        for (final Printable origin : this.origins) {
            result = origin.print(result);
        }
        for (final Map.Entry<String, Integer> number : this.numbers.entrySet()) {
            result = result.with(number.getKey(), number.getValue());
        }
        for (final Map.Entry<String, ? extends Printable> child : this.children.entrySet()) {
            result = result.with(child.getKey(), child.getValue());
        }
        return result;
    }
}
