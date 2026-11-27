/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

/**
 * The OpenAPI Paths Object, containing HTTP route templates and their items.
 * For example, select a school route before looking up its POST operation.
 *
 * @since 0.0.1
 */
public interface Paths {

    /**
     * Enumerate the declared HTTP path items.
     *
     * @return Items in declaration order
     */
    Iterable<PathItem> iterate();

    /**
     * Select a declared route template.
     *
     * @param template Exact route template
     * @return Matching path item
     */
    PathItem path(String template);
}
