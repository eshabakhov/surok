/*
 * © 2025-2026 Eset Shabakhov. Surok
 */
package com.eshabakhov.surok.security;

import org.springframework.web.bind.annotation.RequestMethod;

/**
 * The OpenAPI Path Item Object for one HTTP route template.
 * For example, a path can describe public GET and protected POST operations.
 *
 * @since 0.0.1
 */
public interface PathItem {

    /**
     * Identify the route containing these operations.
     *
     * @return Route template
     */
    String template();

    /**
     * Enumerate the operations belonging to this path.
     *
     * @return Path operations
     */
    Operations operations();

    /**
     * Select an operation by its HTTP method.
     *
     * @param method HTTP method
     * @return Declared operation
     */
    default Operation operation(final RequestMethod method) {
        return this.operations().operation(this.template(), method);
    }
}
