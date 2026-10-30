/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok;

/**
 * Printable collection items, including an explicitly empty collection.
 * For example, {@code media.with("classes", new PrItems("class", classes))}
 * prints a JSON array or an XML container with repeated class elements.
 *
 * @since 0.0.1
 */
public final class PrItems implements Printable {

    /** Item name. */
    private final String name;

    /** Items to print. */
    private final Iterable<? extends Printable> items;

    /**
     * New printable items.
     *
     * @param name Item name
     * @param items Items to print
     */
    public PrItems(
        final String name,
        final Iterable<? extends Printable> items
    ) {
        this.name = name;
        this.items = items;
    }

    @Override
    public <T> Media<T> print(final Media<T> media) throws Exception {
        return media.items(this.name, this.items);
    }
}
