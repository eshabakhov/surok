/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok;

/**
 * Pagination state enriched with query result information.
 *
 * @since 0.0.1
 */
public interface ResultPage extends Page, Printable {

    /**
     * Total number of matching items.
     *
     * @return Total number of items
     */
    int total();

    /**
     * Total number of pages.
     *
     * @return Total number of pages
     */
    int totalPages();

    /**
     * Indicates whether the next page exists.
     *
     * @return Whether the next page exists
     */
    boolean hasNext();

    /**
     * Indicates whether the previous page exists.
     *
     * @return Whether the previous page exists
     */
    boolean hasPrev();
}
